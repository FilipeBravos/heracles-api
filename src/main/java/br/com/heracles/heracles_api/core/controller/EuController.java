package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MeusDadosDtos;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.NotificacaoDtos;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.service.MinhaAreaService;
import br.com.heracles.heracles_api.core.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public EuController(MinhaAreaService service, NotificacaoService notificacaoService) {
        this.service = service;
        this.notificacaoService = notificacaoService;
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
}
