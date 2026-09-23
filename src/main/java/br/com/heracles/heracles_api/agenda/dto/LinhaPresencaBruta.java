package br.com.heracles.heracles_api.agenda.dto;

import java.time.LocalDateTime;

/**
 * Projecao de uma presenca ja confirmada, com o suficiente pra agrupar por
 * horario recorrente no servico (dia da semana + hora, derivados de
 * dataHora em Java, nao em JPQL) — nao ha coluna de dia-da-semana na
 * aula, cada ocorrencia e uma linha propria.
 */
public record LinhaPresencaBruta(
        String nomeAula, Long unidadeId, String unidadeNome, String professorNome,
        LocalDateTime dataHora, Boolean presente) {
}
