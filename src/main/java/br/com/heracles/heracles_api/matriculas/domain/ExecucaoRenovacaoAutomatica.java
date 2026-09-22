package br.com.heracles.heracles_api.matriculas.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Um registro do job diario de renovacao automatica no cartao: quando
 * rodou, e quantas assinaturas cobrou e renovou naquele dia.
 *
 * So historico — sem alerta ativo. Um dia sem registro aqui ja denuncia
 * sozinho que o job nao rodou ou falhou, sem precisar comparar contra
 * nenhum baseline.
 */
@Entity
@Table(name = "execucoes_renovacao_automatica", schema = "matriculas")
@Getter
@Setter
public class ExecucaoRenovacaoAutomatica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_execucao", updatable = false)
    private LocalDateTime dataExecucao;

    @Column(name = "quantidade_renovada")
    private int quantidadeRenovada;

    @PrePersist
    public void prePersist() {
        if (this.dataExecucao == null) {
            this.dataExecucao = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecucaoRenovacaoAutomatica outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
