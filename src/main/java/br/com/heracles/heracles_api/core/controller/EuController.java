package br.com.heracles.heracles_api.core.controller;

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
}
