package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.FormaPagamento;

import java.math.BigDecimal;

/**
 * Atraso medio de pagamento de uma forma de pagamento, entre cobrancas
 * pagas no periodo, do pior pro melhor.
 *
 * Quem paga antes ou no dia do vencimento entra com atraso zero — nunca
 * negativo, adiantar o pagamento nao "compensa" um atraso de outra
 * cobranca na media.
 */
public record LinhaAtrasoPagamento(FormaPagamento formaPagamento, long quantidade, BigDecimal atrasoMedioDias) {
}
