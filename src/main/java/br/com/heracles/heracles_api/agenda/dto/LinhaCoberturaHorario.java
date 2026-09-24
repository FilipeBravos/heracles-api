package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.DiaSemana;

import java.time.LocalTime;

/**
 * Um bloco de 30 minutos, num dia da semana, numa unidade, sem nenhum
 * professor cobrindo — lacuna bruta da agenda, diferente da taxa de
 * ocupacao (LinhaOcupacaoPersonal), que so olha professores que ja tem
 * horario cadastrado.
 */
public record LinhaCoberturaHorario(
        Long unidadeId, String unidadeNome, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFim) {
}
