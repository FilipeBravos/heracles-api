package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MeusDadosDtos;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * O que qualquer usuario autenticado ve e edita de si mesmo.
 *
 * Todo metodo aqui recebe o e-mail do token, nunca um id vindo da
 * requisicao. E a diferenca entre "minhas fichas" e "as fichas de quem
 * eu disser": com um id no caminho, bastaria trocar o numero para ler a
 * ficha de outro aluno.
 *
 * Ficha, matricula e historico so fazem sentido para o perfil ALUNO — os
 * demais recebem lista ou estado vazio, nao erro, porque a pergunta
 * "quais sao minhas fichas" tem resposta valida mesmo para quem nao tem
 * nenhuma. Nome, telefone e senha ja sao de qualquer perfil.
 */
@Service
public class MinhaAreaService {

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final HistoricoTreinoAlunoRepository historicoTreinoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioService usuarioService;

    public MinhaAreaService(UsuarioRepository usuarioRepository,
                            TreinoRepository treinoRepository,
                            AssinaturaRepository assinaturaRepository,
                            HistoricoTreinoAlunoRepository historicoTreinoRepository,
                            PasswordEncoder passwordEncoder,
                            UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.historicoTreinoRepository = historicoTreinoRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<TreinoResponse> minhasFichas(String emailAutenticado) {
        return treinoRepository.fichasDoAluno(eu(emailAutenticado).getId()).stream()
                .map(TreinoResponse::de)
                .toList();
    }

    /**
     * Fichas que ja foram do aluno e nao sao mais.
     *
     * A atual nao entra aqui — "Meu treino" ja a mostra, e repeti-la seria
     * a mesma ficha em dois lugares com nomes diferentes. Nome, foco e
     * nivel vem do registro histórico, nao da ficha viva: ela pode ter
     * sido renomeada ou apagada desde a troca.
     */
    @Transactional(readOnly = true)
    public List<HistoricoTreinoResponse> historicoDeTreinos(String emailAutenticado) {
        return historicoTreinoRepository.historicoDoAluno(eu(emailAutenticado).getId()).stream()
                .map(HistoricoTreinoResponse::de)
                .toList();
    }

    /**
     * Minha matricula: plano, vencimento e situacao.
     *
     * Vale a vigente — a que nao foi cancelada —, que e a unica que
     * responde "meu acesso esta em dia?". O historico de planos antigos e
     * outra pergunta, e quem a faz e o balcao.
     *
     * Nao ter matricula vigente nao e erro: o aluno acabou de ser
     * cadastrado e ainda nao passou na recepcao, ou a matricula foi
     * cancelada. Nos dois casos ha uma resposta a dar, e um 404 obrigaria
     * a tela a tratar o normal como falha.
     */
    @Transactional(readOnly = true)
    public MinhaMatriculaResponse minhaMatricula(String emailAutenticado) {
        return assinaturaRepository.buscarVigentePorAluno(eu(emailAutenticado).getId())
                .map(assinatura -> MinhaMatriculaResponse.de(assinatura, LocalDate.now()))
                .orElseGet(MinhaMatriculaResponse::semMatricula);
    }

    /** Nome, e-mail e telefone de quem esta autenticado. */
    @Transactional(readOnly = true)
    public MeusDadosDtos.Response meusDados(String emailAutenticado) {
        return MeusDadosDtos.Response.de(eu(emailAutenticado));
    }

    /**
     * Atualiza nome e telefone de quem esta autenticado.
     *
     * So isso: e-mail e CPF nao entram no contrato de entrada
     * (MeusDadosDtos.Atualizar), entao nao ha o que sequestrar mudando um
     * dos dois por engano ou de proposito.
     */
    @Transactional
    public MeusDadosDtos.Response atualizarMeusDados(String emailAutenticado, MeusDadosDtos.Atualizar request) {
        Usuario eu = eu(emailAutenticado);
        eu.setNome(request.nome());
        eu.setTelefone(request.telefone());
        return MeusDadosDtos.Response.de(eu);
    }

    /**
     * Troca a propria senha, conferindo a atual antes.
     *
     * A conferencia importa mesmo com o token ja provando quem e o
     * portador: um token roubado sem a senha nao basta para assumir a
     * conta trocando a senha por baixo do dono real.
     */
    @Transactional
    public void trocarSenha(String emailAutenticado, MeusDadosDtos.TrocarSenha request) {
        Usuario eu = eu(emailAutenticado);
        if (!passwordEncoder.matches(request.senhaAtual(), eu.getSenhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta.");
        }
        eu.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
    }

    /**
     * O historico de avaliacoes fisicas de quem esta autenticado.
     *
     * Delega para UsuarioService em vez de repetir a consulta: a regra ja
     * existe la para o balcao, e aqui so muda de onde vem o id do aluno —
     * do token, nunca de um parametro que desse pra trocar.
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoFisicaDtos.Response> minhasAvaliacoesFisicas(String emailAutenticado) {
        return usuarioService.historicoAvaliacoesFisicas(eu(emailAutenticado).getId());
    }

    @Transactional(readOnly = true)
    public AvaliacaoFisicaDtos.Comparativo meuComparativoFisico(String emailAutenticado, Long deId, Long paraId) {
        return usuarioService.compararAvaliacoesFisicas(eu(emailAutenticado).getId(), deId, paraId);
    }

    @Transactional(readOnly = true)
    public AvaliacaoFisicaFoto minhaFotoAvaliacaoFisica(String emailAutenticado, Long avaliacaoId, Long fotoId) {
        return usuarioService.buscarFotoAvaliacaoFisica(eu(emailAutenticado).getId(), avaliacaoId, fotoId);
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));
    }
}
