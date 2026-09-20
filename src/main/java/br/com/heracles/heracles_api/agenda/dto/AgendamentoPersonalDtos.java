package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class AgendamentoPersonalDtos {

    private AgendamentoPersonalDtos() {
    }

    public record Salvar(
            @NotNull(message = "Informe o aluno")
            Long alunoId,

            @NotNull(message = "Informe o professor")
            Long professorId,

            @NotNull(message = "Informe a unidade")
            Long unidadeId,

            @NotNull(message = "Informe a data e hora da sessao")
            @Future(message = "A sessao precisa ser marcada para o futuro")
            LocalDateTime dataHora,

            @NotNull(message = "Informe a duracao da sessao")
            @Positive(message = "Duracao invalida")
            Integer duracaoMinutos,

            @Size(max = 500, message = "As observacoes devem ter no maximo 500 caracteres")
            String observacoes
    ) {
    }

    public record Response(
            Long id,
            Long alunoId,
            String alunoNome,
            Long professorId,
            String professorNome,
            Long unidadeId,
            String unidadeNome,
            LocalDateTime dataHora,
            int duracaoMinutos,
            String observacoes,
            StatusAgendamento status
    ) {
        public static Response de(AgendamentoPersonal sessao) {
            return new Response(
                    sessao.getId(),
                    sessao.getAluno().getId(),
                    sessao.getAluno().getNome(),
                    sessao.getProfessor().getId(),
                    sessao.getProfessor().getNome(),
                    sessao.getUnidade().getId(),
                    sessao.getUnidade().getNome(),
                    sessao.getDataHora(),
                    sessao.getDuracaoMinutos(),
                    sessao.getObservacoes(),
                    sessao.getStatus()
            );
        }
    }
}
