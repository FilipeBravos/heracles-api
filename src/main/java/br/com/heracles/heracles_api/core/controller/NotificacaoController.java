package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.LinhaTaxaLeituraNotificacao;
import br.com.heracles.heracles_api.core.service.NotificacaoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Visibilidade de gestao sobre a central de notificacoes — diferente de
 * /api/eu/notificacoes, que e sempre sobre quem esta autenticado.
 */
@Validated
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    /** Taxa de leitura por tipo de notificacao, do pior pro melhor, com o tempo medio ate a leitura. */
    @GetMapping("/relatorio/taxa-leitura")
    public List<LinhaTaxaLeituraNotificacao> taxaLeitura(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias) {
        return service.taxaLeituraPorTipo(dias);
    }
}
