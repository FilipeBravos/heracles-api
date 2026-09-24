package br.com.heracles.heracles_api.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Projecao de uma avaliacao fisica, com o suficiente pra calcular o delta por aluno em Java. */
public record LinhaAvaliacaoParaEvolucao(
        Long alunoId, LocalDate data, BigDecimal pesoKg, BigDecimal alturaCm, BigDecimal percentualGordura) {
}
