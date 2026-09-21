package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Notificacao;
import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoNotificacao;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.NotificacaoDtos;
import br.com.heracles.heracles_api.core.repository.NotificacaoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A central de notificacoes: quem le as suas, e os jobs que as geram.
 *
 * Sem SMTP nem push configurado no projeto — o aviso fica dentro do
 * proprio app. Os metodos de leitura seguem o mesmo padrao de
 * MinhaAreaService (o e-mail do token, nunca um id da requisicao,
 * decide de quem sao as notificacoes); os de geracao rodam por um job
 * diario (NotificacaoScheduler) e enxergam a base inteira, nao um
 * usuario so.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;

    public NotificacaoService(NotificacaoRepository repository,
                              UsuarioRepository usuarioRepository,
                              AssinaturaRepository assinaturaRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoDtos.Response> listar(String emailAutenticado, Pageable pageable) {
        return repository.findByDestinatarioId(eu(emailAutenticado).getId(), pageable)
                .map(NotificacaoDtos.Response::de);
    }

    @Transactional(readOnly = true)
    public NotificacaoDtos.Resumo resumo(String emailAutenticado) {
        return new NotificacaoDtos.Resumo(
                repository.countByDestinatarioIdAndLidaFalse(eu(emailAutenticado).getId()));
    }

    /**
     * So o dono le e marca a propria notificacao. Sem essa checagem, o id
     * bastaria pra marcar (ou pior, so pra confirmar a existencia de) um
     * aviso de outra pessoa.
     */
    @Transactional
    public void marcarComoLida(String emailAutenticado, Long notificacaoId) {
        Usuario eu = eu(emailAutenticado);
        Notificacao notificacao = repository.findById(notificacaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Notificacao", notificacaoId));
        if (!notificacao.getDestinatario().getId().equals(eu.getId())) {
            throw RecursoNaoEncontradoException.de("Notificacao", notificacaoId);
        }
        notificacao.setLida(true);
    }

    @Transactional
    public void marcarTodasComoLidas(String emailAutenticado) {
        repository.marcarTodasComoLidas(eu(emailAutenticado).getId());
    }

    // ---------------------------------------------------------------
    // Geracao — chamada pelo job diario, nao por uma tela
    // ---------------------------------------------------------------

    /**
     * Avisa o proprio aluno de que a matricula vence em breve.
     *
     * Mesma janela que o relatorio de inadimplencia usa
     * (AssinaturaService.DIAS_VENCE_EM_BREVE_PADRAO) — as duas leituras
     * de "vence em breve" devem concordar.
     */
    @Transactional
    public int gerarNotificacoesMatriculaVencendo(int diasAntes) {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteDaJanela = hoje.plusDays(diasAntes);
        LocalDateTime inicioDoDia = hoje.atStartOfDay();

        int criadas = 0;
        for (Assinatura assinatura : assinaturaRepository.buscarAtivasVencendoEntre(hoje, limiteDaJanela)) {
            Usuario aluno = assinatura.getAluno();
            if (repository.existeDesde(aluno.getId(), TipoNotificacao.MATRICULA_VENCENDO,
                    assinatura.getId(), inicioDoDia)) {
                continue;
            }

            long dias = ChronoUnit.DAYS.between(hoje, assinatura.getDataVencimento());
            Notificacao notificacao = new Notificacao();
            notificacao.setDestinatario(aluno);
            notificacao.setTipo(TipoNotificacao.MATRICULA_VENCENDO);
            notificacao.setTitulo("Matricula vencendo");
            notificacao.setMensagem(dias == 0
                    ? "Sua matricula vence hoje."
                    : "Sua matricula vence em %d dia%s.".formatted(dias, dias == 1 ? "" : "s"));
            notificacao.setReferenciaId(assinatura.getId());
            repository.save(notificacao);
            criadas++;
        }
        return criadas;
    }

    /**
     * Avisa a secretaria — nao o aluno — de quantos alunos aguardam
     * anamnese. E um resumo diario, nao um aviso por aluno: a secretaria
     * quer saber quanto trabalho tem pela frente, nao ser inundada com
     * uma notificacao por pendencia.
     */
    @Transactional
    public int gerarNotificacoesAnamnesePendente() {
        List<Usuario> semAnamnese = usuarioRepository.buscarAlunosSemAnamnese();
        if (semAnamnese.isEmpty()) {
            return 0;
        }

        LocalDateTime inicioDoDia = LocalDate.now().atStartOfDay();
        int criadas = 0;
        for (Usuario secretaria : secretariasAtivas()) {
            if (repository.existeDesde(secretaria.getId(), TipoNotificacao.ANAMNESE_PENDENTE, null, inicioDoDia)) {
                continue;
            }

            Notificacao notificacao = new Notificacao();
            notificacao.setDestinatario(secretaria);
            notificacao.setTipo(TipoNotificacao.ANAMNESE_PENDENTE);
            notificacao.setTitulo("Anamnese pendente");
            notificacao.setMensagem(semAnamnese.size() == 1
                    ? "1 aluno aguardando anamnese."
                    : "%d alunos aguardando anamnese.".formatted(semAnamnese.size()));
            repository.save(notificacao);
            criadas++;
        }
        return criadas;
    }

    /**
     * Avisa a secretaria de quem faz aniversario hoje — util pra ela
     * cumprimentar o aluno ou oferecer alguma promocao, nao informacao
     * que o proprio aniversariante precisa de alguem mais.
     */
    @Transactional
    public int gerarNotificacoesAniversario() {
        LocalDate hoje = LocalDate.now();
        List<Usuario> aniversariantes =
                usuarioRepository.buscarAniversariantesDoDia(hoje.getMonthValue(), hoje.getDayOfMonth());
        if (aniversariantes.isEmpty()) {
            return 0;
        }

        LocalDateTime inicioDoDia = hoje.atStartOfDay();
        List<Usuario> secretarias = secretariasAtivas();
        int criadas = 0;
        for (Usuario aluno : aniversariantes) {
            for (Usuario secretaria : secretarias) {
                if (repository.existeDesde(secretaria.getId(), TipoNotificacao.ANIVERSARIO, aluno.getId(), inicioDoDia)) {
                    continue;
                }

                Notificacao notificacao = new Notificacao();
                notificacao.setDestinatario(secretaria);
                notificacao.setTipo(TipoNotificacao.ANIVERSARIO);
                notificacao.setTitulo("Aniversario de aluno");
                notificacao.setMensagem("Hoje e aniversario de %s.".formatted(aluno.getNome()));
                notificacao.setReferenciaId(aluno.getId());
                repository.save(notificacao);
                criadas++;
            }
        }
        return criadas;
    }

    /**
     * Avisa o aluno de que uma vaga surgiu na aula em que ele esperava e
     * que ele foi inscrito automaticamente. Gerada na hora, pelo proprio
     * cancelamento que abriu a vaga (AulaGrupoService) — nao pelo job
     * diario, que so olha para tras uma vez por dia.
     */
    @Transactional
    public void notificarVagaLiberada(Usuario aluno, Long aulaId, String nomeAula) {
        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatario(aluno);
        notificacao.setTipo(TipoNotificacao.VAGA_LIBERADA);
        notificacao.setTitulo("Vaga liberada");
        notificacao.setMensagem(
                "Uma vaga surgiu na aula \"%s\" e voce foi inscrito(a) automaticamente.".formatted(nomeAula));
        notificacao.setReferenciaId(aulaId);
        repository.save(notificacao);
    }

    private List<Usuario> secretariasAtivas() {
        return usuarioRepository.findByTipoPerfilAndStatus(TipoPerfil.SECRETARIA, StatusUsuario.ATIVO);
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));
    }
}
