package br.com.heracles.heracles_api.operacoes.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Item a venda no balcao. O estoque e por unidade. */
@Entity
@Table(name = "produtos_suplementos", schema = "operacoes")
@Getter
@Setter
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    private String nome;
    private String marca;

    @Column(name = "preco_venda")
    private BigDecimal precoVenda;

    @Column(name = "quantidade_estoque")
    private Integer quantidadeEstoque;

    /** Abaixo disso, o produto entra na sugestao de reposicao — limiar proprio, nao um valor global pra todo produto. */
    @Column(name = "estoque_minimo")
    private Integer estoqueMinimo;

    /**
     * Produto sai de linha em vez de ser apagado: o historico de vendas
     * aponta para ele, e o item vendido precisa continuar rastreavel.
     */
    private Boolean ativo = true;

    /**
     * Baixa o estoque de uma venda.
     *
     * A verificacao vive na entidade, e nao no servico, porque ela e a
     * unica que pode garantir a invariante junto com o estado — e o banco
     * repete a regra num CHECK, para o caso de alguem escrever por fora.
     */
    public void baixarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade vendida precisa ser maior que zero.");
        }
        if (this.quantidadeEstoque < quantidade) {
            throw new RegraNegocioException(
                    "Estoque insuficiente de \"%s\": restam %d.".formatted(this.nome, this.quantidadeEstoque));
        }
        this.quantidadeEstoque -= quantidade;
    }

    public boolean isAtivo() {
        return Boolean.TRUE.equals(this.ativo);
    }

    /** Quantas unidades faltam pra alcancar o estoque minimo — zero quando ja esta em dia ou acima. */
    public int quantidadeParaRepor() {
        return Math.max(0, this.estoqueMinimo - this.quantidadeEstoque);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
