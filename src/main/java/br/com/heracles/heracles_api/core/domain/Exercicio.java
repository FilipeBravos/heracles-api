package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exercicios", schema = "core")
@Getter
@Setter
public class Exercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    /** Numero de series prescritas. */
    private Integer series;

    /**
     * Faixa de repeticoes por serie. Quando a prescricao e exata ("3x12"),
     * min e max sao iguais; quando e uma faixa ("4x10 a 12"), diferem.
     */
    @Column(name = "repeticoes_min")
    private Integer repeticoesMin;

    @Column(name = "repeticoes_max")
    private Integer repeticoesMax;

    /**
     * Prescricao de carga, em texto: o treino e um modelo compartilhado entre
     * alunos, entao o que cabe aqui e "ate a falha" ou "70% 1RM", nao um peso.
     */
    private String carga;

    private String observacoes;

    /** Posicao do exercicio dentro da ficha (0-based). */
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treino_id")
    @JsonIgnore
    private Treino treino;

    /** Repeticoes totais prescritas. Base para relatorio de volume. */
    @Transient
    public int volumePrescritoMinimo() {
        return series * repeticoesMin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exercicio outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
