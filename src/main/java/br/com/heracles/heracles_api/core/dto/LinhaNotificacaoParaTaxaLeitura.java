package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoNotificacao;

import java.time.LocalDateTime;

/**
 * Projecao de uma notificacao, com o suficiente pra calcular a taxa de
 * leitura e o tempo medio ate a leitura no servico — lidaEm e nula tanto
 * pra quem nunca leu quanto pra quem leu antes da coluna existir.
 */
public record LinhaNotificacaoParaTaxaLeitura(
        TipoNotificacao tipo, boolean lida, LocalDateTime criadaEm, LocalDateTime lidaEm) {
}
