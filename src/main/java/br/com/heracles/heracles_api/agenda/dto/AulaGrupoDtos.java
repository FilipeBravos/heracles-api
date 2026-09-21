package br.com.heracles.heracles_api.agenda.dto;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /** Visao operacional: quem monta/gerencia a agenda, com a ocupacao e a fila de espera da turma. */
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
            long vagasEspera,
            StatusAula status
    ) {
        public static Response de(AulaGrupo aula, long vagasOcupadas, long vagasEspera) {
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
                    vagasEspera,
                    aula.getStatus()
            );
        }
    }

    /** Visao do aluno: sem o professor como "dado de gestao", com se ele mesmo esta inscrito ou em que posicao da espera esta. */
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
            /** Posicao (1-based) na fila de espera, ou null se nao esta nela. */
            Integer posicaoEspera,
            StatusAula status
    ) {
        public static ParaAluno de(AulaGrupo aula, long vagasOcupadas, boolean inscrito, Integer posicaoEspera) {
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
                    posicaoEspera,
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

    /**
     * O que aconteceu ao tentar marcar a vaga: entrou direto (INSCRITA) ou
     * a turma estava cheia e foi para a fila (EM_ESPERA, com a posicao).
     */
    public record ResultadoInscricao(StatusInscricao status, Integer posicaoEspera) {
        public static ResultadoInscricao inscrito() {
            return new ResultadoInscricao(StatusInscricao.INSCRITA, null);
        }

        public static ResultadoInscricao emEspera(int posicao) {
            return new ResultadoInscricao(StatusInscricao.EM_ESPERA, posicao);
        }
    }

    /** Uma vaga marcada no roster da aula — o que o professor confere pra confirmar presenca. */
    public record LinhaPresenca(Long alunoId, String alunoNome, Boolean presente) {
        public static LinhaPresenca de(InscricaoAula inscricao) {
            return new LinhaPresenca(
                    inscricao.getAluno().getId(), inscricao.getAluno().getNome(), inscricao.getPresente());
        }
    }

    /** Corpo da confirmacao de presenca — quem esteve la e quem sabe. */
    public record ConfirmarPresenca(
            @NotNull(message = "Informe se o aluno compareceu")
            Boolean presente
    ) {
    }

    /**
     * O relatorio de faltas: taxa de comparecimento geral e o ranking de
     * quem mais falta — a mesma pergunta de "avaliacao por professor",
     * mas do lado de quem marca a vaga e nao aparece.
     */
    public record PainelPresenca(
            int dias,
            long totalConfirmadas,
            long totalFaltas,
            BigDecimal taxaComparecimento,
            /** Do que mais falta pro que menos, limitado aos top 10. */
            List<LinhaFaltaAluno> maisFaltosos
    ) {
    }
}
