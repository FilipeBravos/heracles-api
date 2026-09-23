package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.FormaPagamento;

import java.time.LocalDate;

/** Projecao de uma cobranca paga no periodo, com o suficiente pra calcular o atraso em Java. */
public record LinhaCobrancaPagaParaAtraso(FormaPagamento formaPagamento, LocalDate dataVencimento, LocalDate dataPagamento) {
}
