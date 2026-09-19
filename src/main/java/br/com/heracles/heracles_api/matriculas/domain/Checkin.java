package br.com.heracles.heracles_api.matriculas.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Um registro de frequencia: o aluno perguntou na catraca/recepcao se
 * podia treinar, e este e o veredito que ficou gravado.
 *
 * Nasce como efeito colateral de AssinaturaService.conferirAcesso — o
 * mesmo veredito que a tela devolve na hora tambem vira historico. Grava
 * tanto liberado quanto barrado, de proposito: uma tentativa barrada e
 * frequencia tambem, e e o que explica pra secretaria por que o aluno
 * reclamou na porta.
 */
@Entity
@Table(name = "checkins", schema = "matriculas")
@Getter
@Setter
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    /** Nulo quando o motivo e SEM_MATRICULA — nao ha assinatura para apontar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id")
    private Assinatura assinatura;

    @Column(name = "momento", updatable = false)
    private LocalDateTime momento;

    private boolean liberado;

    @Enumerated(EnumType.STRING)
    private MotivoAcesso motivo;

    @PrePersist
    public void prePersist() {
        if (this.momento == null) this.momento = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Checkin outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
