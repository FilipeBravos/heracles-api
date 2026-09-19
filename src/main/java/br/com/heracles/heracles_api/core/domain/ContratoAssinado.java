package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * O contrato de adesão assinado eletronicamente no cadastro do aluno.
 *
 * Substitui "só senha inicial" como o momento de aceite: a senha autentica
 * quem ele é depois, mas nunca disse que ele concordou com os termos. Uma
 * linha por aluno — o contrato assinado no cadastro não se reescreve, só se
 * consulta depois. `textoContrato` grava o texto vigente no momento da
 * assinatura, não uma referência a um texto que a rede pode editar amanhã:
 * o que o aluno assinou precisa continuar legível mesmo se o modelo mudar.
 */
@Entity
@Table(name = "contratos_assinados", schema = "core")
@Getter
@Setter
public class ContratoAssinado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", unique = true)
    private Usuario aluno;

    @Column(name = "nome_digitado")
    private String nomeDigitado;

    @Column(name = "texto_contrato", columnDefinition = "text")
    private String textoContrato;

    @Column(name = "assinado_em", updatable = false)
    private LocalDateTime assinadoEm;

    /** IP de quem preencheu o formulário — normalmente o balcão, não o aluno. */
    @Column(name = "ip_origem")
    private String ipOrigem;

    @PrePersist
    public void prePersist() {
        if (this.assinadoEm == null) this.assinadoEm = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContratoAssinado outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
