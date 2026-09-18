package br.com.heracles.heracles_api.matriculas.domain;

import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Matricula de um aluno num plano.
 *
 * O ciclo e: matricular (ATIVA), renovar (empurra o vencimento), marcar
 * inadimplente quando o pagamento nao entra, e cancelar (terminal). O
 * banco garante, por indice parcial, que um aluno nao tenha duas
 * matriculas vigentes ao mesmo tempo.
 */
@Entity
@Table(name = "assinaturas_alunos", schema = "matriculas")
@Getter
@Setter
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @Enumerated(EnumType.STRING)
    private OrigemAssinatura origem;

    /** Identificador do aluno no parceiro. Nulo em matricula direta. */
    @Column(name = "token_parceiro")
    private String tokenParceiro;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusAssinatura status = StatusAssinatura.ATIVA;

    @Column(name = "data_cancelamento")
    private LocalDate dataCancelamento;

    /** Como o aluno paga cada ciclo — reaproveitado em toda cobranca gerada para esta assinatura. */
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    /**
     * Vencida quando o dia de hoje ja passou do vencimento.
     *
     * Estritamente depois: no proprio dia do vencimento o aluno ainda
     * treina — ele pagou por aquele dia.
     */
    public boolean estaVencidaEm(LocalDate hoje) {
        return hoje.isAfter(this.dataVencimento);
    }

    /**
     * Libera a catraca?
     *
     * INADIMPLENTE nao libera — marcar inadimplente existe justamente
     * para interromper o acesso sem apagar a matricula do aluno.
     */
    public boolean permiteAcessoEm(LocalDate hoje) {
        return this.status == StatusAssinatura.ATIVA && !estaVencidaEm(hoje);
    }

    /**
     * Empurra o vencimento por um periodo do plano e devolve a assinatura
     * a ATIVA.
     *
     * O periodo conta a partir do vencimento atual ou de hoje, o que for
     * mais tarde. Os dois lados importam: quem paga adiantado nao perde
     * os dias que ainda tinha, e quem ficou tres meses parado nao renova
     * para uma data no passado — renovaria ja vencido.
     */
    public void renovar(LocalDate hoje) {
        if (this.status == StatusAssinatura.CANCELADA) {
            throw new RegraNegocioException(
                    "Assinatura cancelada nao se renova. Matricule o aluno novamente.");
        }
        LocalDate base = this.dataVencimento.isAfter(hoje) ? this.dataVencimento : hoje;
        this.dataVencimento = base.plusMonths(this.plano.getTipoCobranca().getMesesDeVigencia());
        this.status = StatusAssinatura.ATIVA;
    }

    public void marcarInadimplente() {
        if (this.status != StatusAssinatura.ATIVA) {
            throw new RegraNegocioException(
                    "So uma assinatura ativa pode ser marcada como inadimplente.");
        }
        this.status = StatusAssinatura.INADIMPLENTE;
    }

    public void cancelar(LocalDate hoje) {
        if (this.status == StatusAssinatura.CANCELADA) {
            throw new RegraNegocioException("Esta assinatura ja esta cancelada.");
        }
        this.status = StatusAssinatura.CANCELADA;
        this.dataCancelamento = hoje;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assinatura outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
