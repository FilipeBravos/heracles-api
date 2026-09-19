package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.DiaSemana;
import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public final class HorarioProfessorDtos {

    private HorarioProfessorDtos() {
    }

    public record Salvar(
            @NotNull(message = "Informe o dia da semana")
            DiaSemana diaSemana,

            @NotNull(message = "Informe o horario de inicio")
            LocalTime horaInicio,

            @NotNull(message = "Informe o horario de fim")
            LocalTime horaFim,

            @NotNull(message = "Informe a unidade")
            Long unidadeId
    ) {
    }

    public record Response(
            Long id,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            Long unidadeId,
            String unidadeNome
    ) {
        public static Response de(HorarioProfessor horario) {
            return new Response(
                    horario.getId(),
                    horario.getDiaSemana(),
                    horario.getHoraInicio(),
                    horario.getHoraFim(),
                    horario.getUnidade().getId(),
                    horario.getUnidade().getNome()
            );
        }
    }
}
