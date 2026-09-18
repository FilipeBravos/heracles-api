package br.com.heracles.heracles_api.matriculas.domain;

import br.com.heracles.heracles_api.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A cobranca de um ciclo da assinatura — boleto, PIX ou cartao.
 *
 * Simulada de proposito: nao ha gateway de pagamento integrado, entao
 * "pagar" aqui e um botao da secretaria (ou um teste automatizado) dizendo
 * que o dinheiro entrou, nao um webhook de verdade. `codigoSimulado` existe
 * so para a tela parecer uma cobranca real — nunca compensa nada.
 *
 * Uma assinatura tem no maximo uma cobranca PENDENTE por vez (indice
 * parcial na migracao): a proxima so nasce quando esta e paga (renovar) ou
 * cancelada (a assinatura foi cancelada primeiro).
 */
@Entity
@Table(name = "cobrancas", schema = "matriculas")
@Getter
@Setter
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assinatura_id")
    private Assinatura assinatura;

    /** Copiado do plano no momento da criacao: o preco pago fica rastreavel mesmo se o plano mudar depois. */
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusCobranca status = StatusCobranca.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    /** Boleto/PIX "copia e cola" fake — so para a tela ter o que mostrar. Nulo no cartao. */
    @Column(name = "codigo_simulado")
    private String codigoSimulado;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) this.dataCriacao = LocalDateTime.now();
        if (this.status == null) this.status = StatusCobranca.PENDENTE;
    }

    public void confirmarPagamento(LocalDate hoje) {
        if (this.status != StatusCobranca.PENDENTE) {
            throw new RegraNegocioException("Esta cobranca nao esta pendente.");
        }
        this.status = StatusCobranca.PAGA;
        this.dataPagamento = hoje;
    }

    public void cancelar() {
        if (this.status == StatusCobranca.PAGA) {
            throw new RegraNegocioException("Uma cobranca ja paga nao se cancela.");
        }
        this.status = StatusCobranca.CANCELADA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cobranca outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
