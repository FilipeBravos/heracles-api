package br.com.heracles.heracles_api.operacoes.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aparelho do salao. O status e derivado dos chamados de manutencao. */
@Entity
@Table(name = "equipamentos", schema = "operacoes")
@Getter
@Setter
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atual")
    private StatusEquipamento statusAtual = StatusEquipamento.OK;

    /** Nulo: sem acompanhamento preventivo configurado pra este equipamento. */
    @Column(name = "intervalo_dias_manutencao")
    private Integer intervaloDiasManutencao;

    /** Ancora a manutencao preventiva quando o equipamento nunca teve chamado resolvido. */
    @Column(name = "cadastrado_em", updatable = false)
    private LocalDateTime cadastradoEm;

    @PrePersist
    public void prePersist() {
        if (this.cadastradoEm == null) {
            this.cadastradoEm = LocalDateTime.now();
        }
    }

    public boolean estaEmManutencao() {
        return this.statusAtual == StatusEquipamento.EM_MANUTENCAO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipamento outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
