package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoNotificacao;

import java.math.BigDecimal;

/**
 * A taxa de leitura de um tipo de notificacao no periodo, do pior pro
 * melhor, e o tempo medio ate a leitura entre as que tem lidaEm registrada.
 *
 * tempoMedioLeituraHoras e nulo quando nenhuma das lidas tem lidaEm —
 * todas foram lidas antes da coluna existir, ou nenhuma foi lida ainda.
 */
public record LinhaTaxaLeituraNotificacao(
        TipoNotificacao tipo, long total, long lidas, BigDecimal taxaLeitura, BigDecimal tempoMedioLeituraHoras) {
}
