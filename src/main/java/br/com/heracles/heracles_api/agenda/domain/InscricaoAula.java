package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Usuario;
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

    @PrePersist
    public void prePersist() {
        if (this.inscritoEm == null) this.inscritoEm = LocalDateTime.now();
    }

    public void cancelar(LocalDateTime agora) {
        this.status = StatusInscricao.CANCELADA;
        this.canceladoEm = agora;
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
