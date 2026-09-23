package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;

import java.time.LocalDateTime;

/**
 * Projecao de uma sessao de personal ja finalizada (realizada ou cancelada),
 * com o suficiente pra calcular a taxa de cancelamento em cima da hora no
 * servico — canceladoEm e nulo tanto para sessoes realizadas quanto para
 * canceladas antes da coluna existir.
 */
public record LinhaSessaoPersonalFinalizada(
        Long professorId, String professorNome, StatusAgendamento status,
        LocalDateTime dataHora, LocalDateTime canceladoEm) {
}
