package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma foto de evolucao dentro de uma avaliacao fisica.
 *
 * Varias por avaliacao (frente, lado, costas, por exemplo) — por isso
 * uma tabela propria em vez de uma coluna na avaliacao, mas o mesmo
 * padrao bytes-no-banco (ver o comentario equivalente em Usuario.foto).
 */
@Entity
@Table(name = "avaliacoes_fisicas_fotos", schema = "core")
@Getter
@Setter
public class AvaliacaoFisicaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id")
    @JsonIgnore
    private AvaliacaoFisica avaliacao;

    /** Sem @Lob de proposito — ver o comentario equivalente em Usuario.foto. */
    private byte[] foto;

    @Column(name = "foto_content_type")
    private String fotoContentType;

    /** Ordem de exibicao — a ordem de upload, 0-based. */
    private Integer ordem;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) this.dataCriacao = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvaliacaoFisicaFoto outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
