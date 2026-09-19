package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Um aviso na central de notificacoes do destinatario.
 *
 * Nao ha e-mail nem push aqui — sem servidor SMTP nem servico de push
 * configurado no projeto, a notificacao vive dentro do proprio app,
 * gerada por jobs diarios (ver NotificacaoService) e lida no sino da
 * barra superior.
 *
 * `referenciaId` aponta pro que originou o aviso — a assinatura que
 * esta vencendo, o aluno que faz aniversario — sem chave estrangeira
 * de verdade porque o alvo muda conforme o tipo. Nulo no resumo diario
 * de anamnese pendente, que nao e sobre um aluno so.
 */
@Entity
@Table(name = "notificacoes", schema = "core")
@Getter
@Setter
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    private String titulo;

    private String mensagem;

    private boolean lida;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "criada_em", updatable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    public void prePersist() {
        if (this.criadaEm == null) this.criadaEm = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacao outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
