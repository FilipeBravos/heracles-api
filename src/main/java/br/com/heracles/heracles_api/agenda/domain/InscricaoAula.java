package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A vaga de um aluno numa aula em grupo — marcada por ele mesmo (o app,
 * self-service) ou pela secretaria em nome dele (o balcao, para quem liga
 * ou passa sem usar o app). Cancelar nao apaga: vira CANCELADA, com a
 * mesma razao de Assinatura nunca ser apagada — o historico de quem
 * frequentou o que fica de pe.
 */
@Entity
@Table(name = "inscricoes_aula", schema = "agenda")
@Getter
@Setter
public class InscricaoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id")
    private AulaGrupo aula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @Enumerated(EnumType.STRING)
    private StatusInscricao status;

    @Column(name = "inscrito_em", updatable = false)
    private LocalDateTime inscritoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    /** Nulo ate o professor confirmar; true = compareceu, false = faltou. */
    private Boolean presente;

    @Column(name = "presenca_confirmada_em")
    private LocalDateTime presencaConfirmadaEm;

    @PrePersist
    public void prePersist() {
        if (this.inscritoEm == null) this.inscritoEm = LocalDateTime.now();
    }

    public void cancelar(LocalDateTime agora) {
        this.status = StatusInscricao.CANCELADA;
        this.canceladoEm = agora;
    }

    /**
     * So o professor que deu a aula confirma quem compareceu — e so depois
     * que ela aconteceu, uma vez so. Quem esta na fila de espera ou
     * cancelou nunca teve vaga de fato, entao nao ha presenca a confirmar.
     */
    public void confirmarPresenca(boolean compareceu, LocalDateTime agora) {
        if (this.status != StatusInscricao.INSCRITA) {
            throw new RegraNegocioException("Esta vaga nao estava marcada — nao ha presenca a confirmar.");
        }
        if (agora.isBefore(this.aula.getFim())) {
            throw new RegraNegocioException("Esta aula ainda nao aconteceu.");
        }
        if (this.presente != null) {
            throw new RegraNegocioException("A presenca ja foi confirmada.");
        }
        this.presente = compareceu;
        this.presencaConfirmadaEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InscricaoAula outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
