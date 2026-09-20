package br.com.heracles.heracles_api.matriculas.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * O registro de um lembrete de vencimento/inadimplencia.
 *
 * Simulado de proposito, no mesmo espirito de Cobranca: nao ha WhatsApp
 * nem SMTP integrado no projeto, entao "enviar" aqui e so gravar o que
 * teria saido — canal e destinatario no momento do envio — sem nenhuma
 * chamada externa de verdade.
 *
 * No maximo um lembrete por (assinatura, estagio) — indice unico na
 * migracao. Virar de estagio (vence em breve -> vencida -> inadimplente)
 * e o que libera um lembrete novo; o mesmo estagio nunca repete.
 */
@Entity
@Table(name = "lembretes_enviados", schema = "matriculas")
@Getter
@Setter
public class LembreteEnviado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assinatura_id")
    private Assinatura assinatura;

    @Enumerated(EnumType.STRING)
    private EstagioLembrete estagio;

    @Enumerated(EnumType.STRING)
    private CanalLembrete canal;

    /** Telefone ou e-mail usado, capturado no momento do envio — o cadastro do aluno pode mudar depois. */
    private String destinatario;

    @Column(name = "data_envio", updatable = false)
    private LocalDateTime dataEnvio;

    @PrePersist
    public void prePersist() {
        if (this.dataEnvio == null) this.dataEnvio = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LembreteEnviado outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
