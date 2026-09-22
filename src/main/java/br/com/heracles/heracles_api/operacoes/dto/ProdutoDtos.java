package br.com.heracles.heracles_api.operacoes.dto;

import br.com.heracles.heracles_api.operacoes.domain.Produto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class ProdutoDtos {

    private ProdutoDtos() {
    }

    public record Request(
            @NotNull(message = "Informe a unidade do produto")
            Long unidadeId,

            @NotBlank(message = "O nome do produto e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @Size(max = 100, message = "A marca deve ter no maximo 100 caracteres")
            String marca,

            @NotNull(message = "Informe o preco de venda")
            @DecimalMin(value = "0.01", message = "O preco precisa ser maior que zero")
            @Digits(integer = 8, fraction = 2, message = "Preco invalido")
            BigDecimal precoVenda,

            @NotNull(message = "Informe a quantidade em estoque")
            @Min(value = 0, message = "O estoque nao pode ser negativo")
            @Max(value = 1_000_000, message = "Quantidade acima do limite")
            Integer quantidadeEstoque,

            @NotNull(message = "Informe o estoque minimo")
            @Min(value = 0, message = "O estoque minimo nao pode ser negativo")
            @Max(value = 1_000_000, message = "Quantidade acima do limite")
            Integer estoqueMinimo
    ) {
    }

    /** Entrada de estoque: soma ao saldo, em vez de sobrescrever. */
    public record AjusteEstoque(
            @NotNull(message = "Informe a quantidade")
            @Min(value = 1, message = "A entrada precisa ser de pelo menos 1")
            @Max(value = 100_000, message = "Quantidade acima do limite")
            Integer quantidade
    ) {
    }

    public record Response(
            Long id,
            Long unidadeId,
            String unidadeNome,
            String nome,
            String marca,
            BigDecimal precoVenda,
            Integer quantidadeEstoque,
            Integer estoqueMinimo,
            boolean ativo
    ) {
        public static Response de(Produto produto) {
            return new Response(
                    produto.getId(),
                    produto.getUnidade().getId(),
                    produto.getUnidade().getNome(),
                    produto.getNome(),
                    produto.getMarca(),
                    produto.getPrecoVenda(),
                    produto.getQuantidadeEstoque(),
                    produto.getEstoqueMinimo(),
                    produto.isAtivo()
            );
        }
    }

    /** Uma linha da sugestao de reposicao: produto abaixo do proprio estoque minimo, e quanto falta pra completar. */
    public record LinhaReposicao(
            Long produtoId,
            String produtoNome,
            String marca,
            Long unidadeId,
            String unidadeNome,
            Integer quantidadeEstoque,
            Integer estoqueMinimo,
            int quantidadeSugerida
    ) {
        public static LinhaReposicao de(Produto produto) {
            return new LinhaReposicao(
                    produto.getId(),
                    produto.getNome(),
                    produto.getMarca(),
                    produto.getUnidade().getId(),
                    produto.getUnidade().getNome(),
                    produto.getQuantidadeEstoque(),
                    produto.getEstoqueMinimo(),
                    produto.quantidadeParaRepor()
            );
        }
    }
}
