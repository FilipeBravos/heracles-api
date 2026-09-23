package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.DiaSemana;

import java.math.BigDecimal;

/**
 * Taxa de no-show de um horario recorrente (mesma aula, mesma unidade,
 * mesmo dia da semana e hora), do pior pro melhor — pra decisao de
 * agenda: horario que ninguem aparece nao vale a pena manter reservado.
 *
 * `ocorrencias` conta datas distintas, nao inscricoes: uma aula com 10
 * alunos confirmados no mesmo dia e uma ocorrencia so.
 */
public record LinhaNoShowPorHorario(
        String nomeAula,
        Long unidadeId,
        String unidadeNome,
        String professorNome,
        DiaSemana diaSemana,
        String horario,
        long ocorrencias,
        long faltas,
        long presencas,
        BigDecimal taxaNoShow
) {
}
