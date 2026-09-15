package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos.MotivoAcesso;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssinaturaService {

    private final AssinaturaRepository repository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;

    public AssinaturaService(AssinaturaRepository repository,
                             PlanoRepository planoRepository,
                             UsuarioRepository usuarioRepository,
                             UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.planoRepository = planoRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public Page<AssinaturaDtos.Response> listar(Pageable pageable) {
        LocalDate hoje = LocalDate.now();
        return repository.buscarPaginadoCompleto(pageable)
                .map(assinatura -> AssinaturaDtos.Response.de(assinatura, hoje));
    }

    @Transactional(readOnly = true)
    public AssinaturaDtos.Response buscarPorId(Long id) {
        return AssinaturaDtos.Response.de(carregar(id));
    }

    /** Historico completo do aluno, canceladas inclusive. */
    @Transactional(readOnly = true)
    public List<AssinaturaDtos.Response> historicoDoAluno(Long alunoId) {
        carregarAluno(alunoId); // 404 antes de devolver lista vazia de id inexistente
        LocalDate hoje = LocalDate.now();
        return repository.historicoDoAluno(alunoId).stream()
                .map(assinatura -> AssinaturaDtos.Response.de(assinatura, hoje))
                .toList();
    }

    /**
     * Matricula o aluno.
     *
     * O vencimento e calculado aqui, a partir do periodo do plano — nunca
     * vem do cliente. Fosse um campo do corpo, bastaria editar a
     * requisicao para se dar um ano de academia por um mes pago.
     */
    @Transactional
    public AssinaturaDtos.Response matricular(AssinaturaDtos.Matricular request) {
        Usuario aluno = carregarAluno(request.alunoId());

        if (aluno.getTipoPerfil() != TipoPerfil.ALUNO) {
            throw new RegraNegocioException(
                    "So alunos se matriculam. \"%s\" esta cadastrado como %s."
                            .formatted(aluno.getNome(), aluno.getTipoPerfil()));
        }

        repository.buscarVigentePorAluno(aluno.getId()).ifPresent(vigente -> {
            throw new RegraNegocioException(
                    "%s ja tem matricula vigente no plano \"%s\". Cancele a atual antes de matricular de novo."
                            .formatted(aluno.getNome(), vigente.getPlano().getNome()));
        });

        Plano plano = planoRepository.findWithUnidadesById(request.planoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", request.planoId()));

        if (!plano.isAtivo()) {
            throw new RegraNegocioException(
                    "O plano \"%s\" esta fora de linha e nao aceita novas matriculas.".formatted(plano.getNome()));
        }

        String token = normalizarToken(request.origem(), request.tokenParceiro());

        Assinatura assinatura = new Assinatura();
        assinatura.setAluno(aluno);
        assinatura.setPlano(plano);
        assinatura.setOrigem(request.origem());
        assinatura.setTokenParceiro(token);
        assinatura.setStatus(StatusAssinatura.ATIVA);

        LocalDate inicio = request.dataInicio() != null ? request.dataInicio() : LocalDate.now();
        assinatura.setDataInicio(inicio);
        assinatura.setDataVencimento(inicio.plusMonths(plano.getTipoCobranca().getMesesDeVigencia()));

        return AssinaturaDtos.Response.de(repository.save(assinatura));
    }

    /** Pagamento entrou: empurra o vencimento e devolve a assinatura a ATIVA. */
    @Transactional
    public AssinaturaDtos.Response renovar(Long id) {
        Assinatura assinatura = carregar(id);
        assinatura.renovar(LocalDate.now());
        return AssinaturaDtos.Response.de(assinatura);
    }

    /** Pagamento nao entrou: interrompe o acesso sem apagar a matricula. */
    @Transactional
    public AssinaturaDtos.Response marcarInadimplente(Long id) {
        Assinatura assinatura = carregar(id);
        assinatura.marcarInadimplente();
        return AssinaturaDtos.Response.de(assinatura);
    }

    @Transactional
    public AssinaturaDtos.Response cancelar(Long id) {
        Assinatura assinatura = carregar(id);
        assinatura.cancelar(LocalDate.now());
        return AssinaturaDtos.Response.de(assinatura);
    }

    /**
     * Este aluno pode treinar nesta unidade hoje?
     *
     * E a pergunta que a recepcao faz na catraca, e a razao de as tres
     * tabelas existirem. O veredito devolve o motivo: barrar sem dizer
     * por que obriga a abrir a ficha do aluno para descobrir se e atraso,
     * vencimento ou plano que nao cobre a unidade — tres encaminhamentos
     * diferentes.
     *
     * A ordem das verificacoes e deliberada: primeiro o que o aluno deve
     * (atraso, vencimento), que a recepcao resolve no balcao, e so depois
     * a cobertura do plano, que e conversa de troca de plano.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.Acesso conferirAcesso(Long alunoId, Long unidadeId) {
        Usuario aluno = carregarAluno(alunoId);
        var unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", unidadeId));

        LocalDate hoje = LocalDate.now();

        Assinatura assinatura = repository.buscarVigentePorAluno(alunoId).orElse(null);
        if (assinatura == null) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.SEM_MATRICULA,
                    "%s nao tem matricula vigente.".formatted(aluno.getNome()), null);
        }

        AssinaturaDtos.Response resumo = AssinaturaDtos.Response.de(assinatura, hoje);

        if (assinatura.getStatus() == StatusAssinatura.INADIMPLENTE) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.INADIMPLENTE,
                    "Matricula em atraso de pagamento.", resumo);
        }
        if (assinatura.estaVencidaEm(hoje)) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.VENCIDA,
                    "Matricula vencida em %s.".formatted(assinatura.getDataVencimento()), resumo);
        }
        if (!assinatura.getPlano().daAcessoA(unidadeId)) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.UNIDADE_NAO_COBERTA,
                    "O plano \"%s\" nao cobre a unidade %s."
                            .formatted(assinatura.getPlano().getNome(), unidade.getNome()), resumo);
        }

        return new AssinaturaDtos.Acesso(true, MotivoAcesso.LIBERADO,
                "Acesso liberado ate %s.".formatted(assinatura.getDataVencimento()), resumo);
    }

    /**
     * Espelha no servico a coerencia que o banco exige por CHECK: parceiro
     * sem token nao da para conferir, e token em matricula direta nao
     * significa nada. A mensagem sai como 409 legivel, em vez de a
     * violacao de constraint virar 500.
     */
    private String normalizarToken(OrigemAssinatura origem, String informado) {
        String token = (informado == null || informado.isBlank()) ? null : informado.trim();

        if (origem.exigeTokenParceiro() && token == null) {
            throw new RegraNegocioException(
                    "Matricula via %s exige o codigo do aluno no parceiro.".formatted(origem));
        }
        if (!origem.exigeTokenParceiro() && token != null) {
            throw new RegraNegocioException("Matricula direta nao tem codigo de parceiro.");
        }
        if (token != null && repository.existsByTokenParceiroAndStatusNot(token, StatusAssinatura.CANCELADA)) {
            throw new RegraNegocioException(
                    "Este codigo de parceiro ja esta em uso por outra matricula vigente.");
        }
        return token;
    }

    private Assinatura carregar(Long id) {
        return repository.findWithAlunoAndPlanoById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Assinatura", id));
    }

    private Usuario carregarAluno(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }
}
