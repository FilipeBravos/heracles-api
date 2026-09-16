package br.com.heracles.heracles_api.matriculas.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Plano contratavel.
 *
 * O conjunto de unidades e o que o plano vende: um plano de rede da
 * acesso a todas, um de bairro a uma so. A pergunta que a recepcao faz
 * ("este aluno pode treinar aqui?") se responde aqui.
 */
@Entity
@Table(name = "planos", schema = "matriculas")
@Getter
@Setter
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(name = "valor_mensal")
    private BigDecimal valorMensal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobranca")
    private TipoCobranca tipoCobranca;

    /**
     * Plano sai de linha em vez de ser apagado: assinaturas passadas
     * apontam para ele, e o valor cobrado precisa continuar rastreavel
     * ao plano que o aluno de fato contratou.
     */
    private Boolean ativo = true;

    // LinkedHashSet: Set porque a tabela de juncao tem chave primaria
    // composta e nao aceita repeticao; ordem de insercao preservada para
    // que a tela liste as unidades sempre na mesma ordem.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "plano_unidades",
            schema = "matriculas",
            joinColumns = @JoinColumn(name = "plano_id"),
            inverseJoinColumns = @JoinColumn(name = "unidade_id")
    )
    private Set<Unidade> unidades = new LinkedHashSet<>();

    public boolean isAtivo() {
        return Boolean.TRUE.equals(this.ativo);
    }

    /** O plano cobre esta unidade? */
    public boolean daAcessoA(Long unidadeId) {
        return unidades.stream().anyMatch(unidade -> unidade.getId().equals(unidadeId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Plano outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
