package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.StatusCobranca;

import java.time.LocalDate;

/** Projecao de uma cobranca, com o suficiente pra achar a mais proxima do envio de um lembrete e checar se pagou. */
public record LinhaCobrancaParaEfetividade(
        Long assinaturaId, LocalDate dataVencimento, StatusCobranca status, LocalDate dataPagamento) {
}
