package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class AulaGrupoDtos {

    private AulaGrupoDtos() {
    }

    public record Salvar(
            @NotBlank(message = "Informe o nome da aula")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @NotNull(message = "Informe o professor")
            Long professorId,

            @NotNull(message = "Informe a unidade")
            Long unidadeId,

            @NotNull(message = "Informe a data e hora da aula")
            @Future(message = "A aula precisa ser marcada para o futuro")
            LocalDateTime dataHora,

            @NotNull(message = "Informe a duracao da aula")
            @Positive(message = "Duracao invalida")
            Integer duracaoMinutos,

            @NotNull(message = "Informe a capacidade maxima")
            @Positive(message = "Capacidade invalida")
            Integer capacidadeMaxima
    ) {
    }

    /** Visao operacional: quem monta/gerencia a agenda, com a ocupacao da turma. */
    public record Response(
            Long id,
            String nome,
            Long professorId,
            String professorNome,
            Long unidadeId,
            String unidadeNome,
            LocalDateTime dataHora,
            int duracaoMinutos,
            int capacidadeMaxima,
            long vagasOcupadas,
            StatusAula status
    ) {
        public static Response de(AulaGrupo aula, long vagasOcupadas) {
            return new Response(
                    aula.getId(),
                    aula.getNome(),
                    aula.getProfessor().getId(),
                    aula.getProfessor().getNome(),
                    aula.getUnidade().getId(),
                    aula.getUnidade().getNome(),
                    aula.getDataHora(),
                    aula.getDuracaoMinutos(),
                    aula.getCapacidadeMaxima(),
                    vagasOcupadas,
                    aula.getStatus()
            );
        }
    }

    /** Visao do aluno: sem o professor como "dado de gestao", com se ele mesmo esta inscrito. */
    public record ParaAluno(
            Long id,
            String nome,
            String professorNome,
            String unidadeNome,
            LocalDateTime dataHora,
            int duracaoMinutos,
            int capacidadeMaxima,
            long vagasOcupadas,
            boolean inscrito,
            StatusAula status
    ) {
        public static ParaAluno de(AulaGrupo aula, long vagasOcupadas, boolean inscrito) {
            return new ParaAluno(
                    aula.getId(),
                    aula.getNome(),
                    aula.getProfessor().getNome(),
                    aula.getUnidade().getNome(),
                    aula.getDataHora(),
                    aula.getDuracaoMinutos(),
                    aula.getCapacidadeMaxima(),
                    vagasOcupadas,
                    inscrito,
                    aula.getStatus()
            );
        }
    }

    /** Corpo de quem marca em nome de outro aluno — a secretaria, no balcao. */
    public record Marcar(
            @NotNull(message = "Informe o aluno")
            Long alunoId
    ) {
    }
}
