package br.com.heracles.heracles_api.matriculas.domain;

import br.com.heracles.heracles_api.core.domain.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A recompensa de quem indicou um aluno novo: um desconto fixo, liberado
 * so quando o indicado paga a primeira cobranca — matricula que cancela
 * antes de pagar nada nao gera recompensa.
 *
 * Nasce PENDENTE e fica assim ate a secretaria revisar e aplicar o
 * desconto na proxima cobranca em aberto do indicador. Nao e automatico
 * de proposito: um erro de regra (indicacao duplicada, aluno errado) nao
 * pode sair descontando dinheiro sozinho.
 */
@Entity
@Table(name = "comissoes_indicacao", schema = "matriculas")
@Getter
@Setter
public class ComissaoIndicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A assinatura do aluno indicado — de quem esta comissao "veio". No maximo uma comissao por assinatura. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assinatura_id")
    @JsonIgnore
    private Assinatura assinatura;

    /** Quem indicou, e vai receber o desconto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicador_id")
    @JsonIgnore
    private Usuario indicador;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusComissao status = StatusComissao.PENDENTE;

    /** A cobranca que recebeu o desconto — nula ate a comissao ser aplicada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_aplicada_id")
    @JsonIgnore
    private Cobranca cobrancaAplicada;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
    }

    public void aplicar(Cobranca cobranca) {
        this.status = StatusComissao.APLICADA;
        this.dataResolucao = LocalDateTime.now();
        this.cobrancaAplicada = cobranca;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComissaoIndicacao outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
