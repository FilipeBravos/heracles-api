package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.service.MinhaAreaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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

    public EuController(MinhaAreaService service) {
        this.service = service;
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
}
