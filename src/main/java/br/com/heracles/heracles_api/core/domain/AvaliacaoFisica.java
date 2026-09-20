package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma avaliacao fisica periodica do aluno — peso, medidas e fotos de
 * evolucao, complementar a anamnese.
 *
 * A anamnese e o intake: preenchida uma vez, atualizada quando muda algo
 * de saude. A avaliacao fisica e o oposto — cada visita gera uma linha
 * nova, e a evolucao esta em comparar uma com a anterior, nao em editar
 * a mesma. Por isso nao ha PUT: uma medida errada se corrige com uma
 * avaliacao nova, nao reescrevendo o passado.
 */
@Entity
@Table(name = "avaliacoes_fisicas", schema = "core")
@Getter
@Setter
public class AvaliacaoFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    private LocalDate data;

    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    @Column(name = "altura_cm")
    private BigDecimal alturaCm;

    @Column(name = "percentual_gordura")
    private BigDecimal percentualGordura;

    @Column(name = "circunferencia_cintura")
    private BigDecimal circunferenciaCintura;

    @Column(name = "circunferencia_quadril")
    private BigDecimal circunferenciaQuadril;

    @Column(name = "circunferencia_braco")
    private BigDecimal circunferenciaBraco;

    @Column(name = "circunferencia_coxa")
    private BigDecimal circunferenciaCoxa;

    @Column(name = "circunferencia_peito")
    private BigDecimal circunferenciaPeito;

    private String observacoes;

    // LAZY + @OrderBy: o historico nao carrega fotos, e quando carregadas
    // vem na ordem em que foram enviadas (frente, lado, costas...).
    @OneToMany(mappedBy = "avaliacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @JsonIgnore
    private List<AvaliacaoFisicaFoto> fotos = new ArrayList<>();

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) this.dataCriacao = LocalDateTime.now();
        if (this.data == null) this.data = LocalDate.now();
    }

    public void adicionarFoto(AvaliacaoFisicaFoto foto) {
        foto.setAvaliacao(this);
        foto.setOrdem(this.fotos.size());
        this.fotos.add(foto);
    }

    /** IMC = peso / altura(m)^2 — null quando falta peso ou altura. */
    public BigDecimal calcularImc() {
        if (pesoKg == null || alturaCm == null || alturaCm.signum() == 0) {
            return null;
        }
        BigDecimal alturaM = alturaCm.divide(BigDecimal.valueOf(100));
        return pesoKg.divide(alturaM.multiply(alturaM), 1, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvaliacaoFisica outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
