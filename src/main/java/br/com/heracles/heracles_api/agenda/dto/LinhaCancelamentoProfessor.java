package br.com.heracles.heracles_api.agenda.dto;

import java.math.BigDecimal;

/**
 * A taxa de cancelamento em cima da hora de um professor: quantas das
 * sessoes finalizadas (realizadas ou canceladas) no periodo foram
 * canceladas com menos do limite de horas de antecedencia.
 */
public record LinhaCancelamentoProfessor(
        Long professorId, String professorNome, long totalSessoes,
        long cancelamentosEmCimaDaHora, BigDecimal taxaCancelamento) {
}
