package br.com.heracles.heracles_api.operacoes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chamado de manutencao de um equipamento.
 *
 * O ciclo e: abrir (equipamento vai para EM_MANUTENCAO) e resolver
 * (volta para OK, com custo e data de resolucao). O banco garante, por
 * indice parcial, que um equipamento nao tenha dois chamados abertos.
 */
@Entity
@Table(name = "historico_manutencao", schema = "operacoes")
@Getter
@Setter
public class ChamadoManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id")
    @JsonIgnore
    private Equipamento equipamento;

    @Column(name = "data_chamado", updatable = false)
    private LocalDateTime dataChamado;

    @Column(name = "descricao_problema")
    private String descricaoProblema;

    @Column(name = "custo_reparo")
    private BigDecimal custoReparo;

    @Enumerated(EnumType.STRING)
    private StatusChamado status = StatusChamado.ABERTO;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @PrePersist
    public void prePersist() {
        if (this.dataChamado == null) {
            this.dataChamado = LocalDateTime.now();
        }
    }

    public boolean estaAberto() {
        return this.status == StatusChamado.ABERTO;
    }

    public void resolver(BigDecimal custo) {
        this.status = StatusChamado.RESOLVIDO;
        this.dataResolucao = LocalDateTime.now();
        this.custoReparo = custo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChamadoManutencao outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
