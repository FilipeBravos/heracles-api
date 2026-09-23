package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * O que o aluno realmente executou de um exercicio, numa data.
 *
 * Exercicio so guarda a prescricao, compartilhada entre alunos; isto e o
 * registro individual de carga/series/repeticoes de quem treinou.
 *
 * O nome do exercicio vem copiado no momento do registro, e a referencia
 * e opcional: o professor pode remover o exercicio da ficha (orphanRemoval
 * em Treino.exercicios) sem apagar o historico do aluno.
 */
@Entity
@Table(name = "execucoes_exercicio", schema = "core")
@Getter
@Setter
public class ExecucaoExercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercicio_id")
    private Exercicio exercicio;

    @Column(name = "exercicio_nome")
    private String exercicioNome;

    @Column(name = "data_execucao")
    private LocalDate dataExecucao;

    @Column(name = "series_realizadas")
    private Integer seriesRealizadas;

    @Column(name = "repeticoes_realizadas")
    private Integer repeticoesRealizadas;

    @Column(name = "carga_realizada")
    private BigDecimal cargaRealizada;

    private String observacao;

    @Column(name = "registrado_em", updatable = false)
    private LocalDateTime registradoEm;

    @PrePersist
    public void prePersist() {
        if (this.registradoEm == null) {
            this.registradoEm = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecucaoExercicio outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
