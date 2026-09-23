package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.service.AgendamentoPersonalService;
import br.com.heracles.heracles_api.agenda.service.AulaGrupoService;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.dto.ExecucaoExercicioDtos;
import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MeusDadosDtos;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.NotificacaoDtos;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.service.ExecucaoExercicioService;
import br.com.heracles.heracles_api.core.service.MinhaAreaService;
import br.com.heracles.heracles_api.core.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A area do proprio usuario.
 *
 * Nenhuma rota daqui aceita id de aluno: o sujeito e sempre quem esta
 * autenticado. Um /api/alunos/{id}/treinos seria a mesma informacao com
 * uma porta a mais para fechar.
 */
@RestController
@RequestMapping("/api/eu")
public class EuController {

    private final MinhaAreaService service;
    private final NotificacaoService notificacaoService;
    private final AulaGrupoService aulaGrupoService;
    private final AgendamentoPersonalService agendamentoPersonalService;
    private final ExecucaoExercicioService execucaoExercicioService;

    public EuController(MinhaAreaService service, NotificacaoService notificacaoService,
                        AulaGrupoService aulaGrupoService,
                        AgendamentoPersonalService agendamentoPersonalService,
                        ExecucaoExercicioService execucaoExercicioService) {
        this.service = service;
        this.notificacaoService = notificacaoService;
        this.aulaGrupoService = aulaGrupoService;
        this.agendamentoPersonalService = agendamentoPersonalService;
        this.execucaoExercicioService = execucaoExercicioService;
    }

    /** As fichas vinculadas a quem esta autenticado. */
    @GetMapping("/treinos")
    public List<TreinoResponse> meusTreinos(@AuthenticationPrincipal Jwt jwt) {
        return service.minhasFichas(jwt.getSubject());
    }

    /**
     * Fichas que ja foram de quem esta autenticado e nao sao mais.
     *
     * A atual não entra: já sai em GET /eu/treinos, e repeti-la aqui seria
     * a mesma ficha sob dois nomes.
     */
    @GetMapping("/treinos/historico")
    public List<HistoricoTreinoResponse> historicoDeTreinos(@AuthenticationPrincipal Jwt jwt) {
        return service.historicoDeTreinos(jwt.getSubject());
    }

    /**
     * A matricula vigente de quem esta autenticado: plano, vencimento e
     * situacao.
     *
     * Devolve 200 com `temMatricula: false` quando nao ha nenhuma. Nao ter
     * matricula e um estado que o aluno precisa ler na tela — nao um
     * recurso ausente.
     */
    @GetMapping("/matricula")
    public MinhaMatriculaResponse minhaMatricula(@AuthenticationPrincipal Jwt jwt) {
        return service.minhaMatricula(jwt.getSubject());
    }

    /** Nome, e-mail e telefone de quem esta autenticado. */
    @GetMapping("/dados")
    public MeusDadosDtos.Response meusDados(@AuthenticationPrincipal Jwt jwt) {
        return service.meusDados(jwt.getSubject());
    }

    /** Atualiza nome e telefone de quem esta autenticado. E-mail e CPF nao sao enderecaveis aqui. */
    @PutMapping("/dados")
    public MeusDadosDtos.Response atualizarMeusDados(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestBody @Valid MeusDadosDtos.Atualizar request) {
        return service.atualizarMeusDados(jwt.getSubject(), request);
    }

    /** Troca a propria senha. Exige a atual — ver MinhaAreaService.trocarSenha. */
    @PutMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody @Valid MeusDadosDtos.TrocarSenha request) {
        service.trocarSenha(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    /** A central de notificacoes de quem esta autenticado — sem e-mail nem push, so o sino do app. */
    @GetMapping("/notificacoes")
    public Page<NotificacaoDtos.Response> minhasNotificacoes(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificacaoService.listar(jwt.getSubject(), pageable);
    }

    /** Contagem de nao lidas — o numero que o sino mostra sem abrir a lista. */
    @GetMapping("/notificacoes/resumo")
    public NotificacaoDtos.Resumo resumoNotificacoes(@AuthenticationPrincipal Jwt jwt) {
        return notificacaoService.resumo(jwt.getSubject());
    }

    @PutMapping("/notificacoes/{id}/lida")
    public ResponseEntity<Void> marcarNotificacaoComoLida(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        notificacaoService.marcarComoLida(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notificacoes/lidas")
    public ResponseEntity<Void> marcarTodasNotificacoesComoLidas(@AuthenticationPrincipal Jwt jwt) {
        notificacaoService.marcarTodasComoLidas(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /** Agenda de aulas em grupo, com a marca de quais quem esta autenticado ja reservou. */
    @GetMapping("/aulas")
    public Page<AulaGrupoDtos.ParaAluno> aulas(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return aulaGrupoService.listarParaAluno(jwt.getSubject(), pageable);
    }

    /**
     * Reserva a propria vaga — o self-service do app, sem passar pelo
     * balcao. A resposta diz se entrou direto ou foi para a fila de
     * espera, ja que a turma cheia nao recusa mais.
     */
    @PostMapping("/aulas/{id}/inscricoes")
    public AulaGrupoDtos.ResultadoInscricao inscreverEmAula(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return aulaGrupoService.inscreverEu(jwt.getSubject(), id);
    }

    @DeleteMapping("/aulas/{id}/inscricoes")
    public ResponseEntity<Void> cancelarInscricaoEmAula(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        aulaGrupoService.cancelarInscricaoEu(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * O roster da aula para o professor confirmar presenca — so o
     * professor que a deu enxerga, do jeito que so ele confirma sessao de
     * personal realizada.
     */
    @GetMapping("/aulas/{id}/inscricoes")
    public List<AulaGrupoDtos.LinhaPresenca> inscricoesDaAula(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return aulaGrupoService.listarInscricoesEu(jwt.getSubject(), id);
    }

    /** So o professor que deu a aula confirma quem compareceu — e so depois que ela aconteceu. */
    @PutMapping("/aulas/{id}/inscricoes/{alunoId}/presenca")
    public ResponseEntity<Void> confirmarPresenca(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long alunoId,
            @RequestBody @Valid AulaGrupoDtos.ConfirmarPresenca request) {
        aulaGrupoService.confirmarPresencaEu(jwt.getSubject(), id, alunoId, request.presente());
        return ResponseEntity.noContent().build();
    }

    /**
     * As sessoes de personal de quem esta autenticado. Agendar e cancelar
     * continuam sendo a secretaria, no balcao — mas confirmar que a propria
     * sessao aconteceu (professor) e avaliar uma ja realizada (aluno) sao
     * as duas escritas que este grupo de rotas permite.
     */
    @GetMapping("/sessoes-personal")
    public Page<AgendamentoPersonalDtos.Response> minhasSessoesPersonal(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return agendamentoPersonalService.listarParaAluno(jwt.getSubject(), pageable);
    }

    /**
     * O professor confirma que a propria sessao aconteceu — so ele estava
     * la para atestar. E o que libera o aluno a avaliar.
     */
    @PutMapping("/sessoes-personal/{id}/realizacao")
    public AgendamentoPersonalDtos.Response marcarSessaoPersonalRealizada(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return agendamentoPersonalService.marcarRealizadaEu(jwt.getSubject(), id);
    }

    /**
     * O aluno avalia a propria sessao ja realizada — nota de 1 a 5,
     * comentario opcional. So depois de REALIZADA, e uma vez so.
     */
    @PutMapping("/sessoes-personal/{id}/avaliacao")
    public AgendamentoPersonalDtos.Response avaliarSessaoPersonal(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestBody @Valid AgendamentoPersonalDtos.Avaliar request) {
        return agendamentoPersonalService.avaliarEu(jwt.getSubject(), id, request);
    }

    /** O historico de avaliacoes fisicas de quem esta autenticado — so leitura, quem registra e o professor. */
    @GetMapping("/avaliacoes-fisicas")
    public List<AvaliacaoFisicaDtos.Response> minhasAvaliacoesFisicas(@AuthenticationPrincipal Jwt jwt) {
        return service.minhasAvaliacoesFisicas(jwt.getSubject());
    }

    /** A primeira avaliacao contra a mais recente, ou duas escolhidas via deId/paraId. */
    @GetMapping("/avaliacoes-fisicas/comparativo")
    public AvaliacaoFisicaDtos.Comparativo meuComparativoFisico(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long deId,
            @RequestParam(required = false) Long paraId) {
        return service.meuComparativoFisico(jwt.getSubject(), deId, paraId);
    }

    /** Registra o que o aluno realmente executou de um exercicio da propria ficha — carga, series e repeticoes de fato feitas. */
    @PostMapping("/execucoes")
    public ResponseEntity<ExecucaoExercicioDtos.Response> registrarExecucao(
            @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid ExecucaoExercicioDtos.Request request) {
        return ResponseEntity.ok(execucaoExercicioService.registrar(jwt.getSubject(), request));
    }

    /** A evolucao de um exercicio especifico, do mais recente pro mais antigo. */
    @GetMapping("/execucoes")
    public List<ExecucaoExercicioDtos.Response> minhasExecucoes(
            @AuthenticationPrincipal Jwt jwt, @RequestParam Long exercicioId) {
        return execucaoExercicioService.minhasExecucoes(jwt.getSubject(), exercicioId);
    }

    @GetMapping("/avaliacoes-fisicas/{avaliacaoId}/fotos/{fotoId}")
    public ResponseEntity<byte[]> minhaFotoAvaliacaoFisica(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long avaliacaoId, @PathVariable Long fotoId) {
        AvaliacaoFisicaFoto foto = service.minhaFotoAvaliacaoFisica(jwt.getSubject(), avaliacaoId, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getFotoContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(foto.getFoto());
    }
}
