package br.com.heracles.heracles_api.agenda.dto;

import java.math.BigDecimal;

/**
 * A taxa de ocupacao da agenda de um professor: horas disponiveis (a
 * partir de HorarioProfessor, escaladas pro periodo) contra horas
 * efetivamente ocupadas por sessoes de personal realizadas.
 *
 * So entram professores com pelo menos um HorarioProfessor cadastrado —
 * sem disponibilidade configurada, nao ha contra o que comparar.
 */
public record LinhaOcupacaoPersonal(
        Long professorId, String professorNome,
        BigDecimal horasDisponiveis, BigDecimal horasOcupadas, BigDecimal taxaOcupacao) {
}
