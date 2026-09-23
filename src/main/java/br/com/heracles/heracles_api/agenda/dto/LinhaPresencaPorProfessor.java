package br.com.heracles.heracles_api.agenda.dto;

import java.math.BigDecimal;

/**
 * A taxa de presenca em aula em grupo de um professor no periodo, do pior
 * pro melhor — distinta da taxa de no-show por horario recorrente: aqui o
 * agrupamento e por professor, nao por dia/hora, e mistura todas as aulas
 * que ele da.
 */
public record LinhaPresencaPorProfessor(
        String professorNome, long totalConfirmadas, long faltas, long presencas, BigDecimal taxaPresenca) {
}
