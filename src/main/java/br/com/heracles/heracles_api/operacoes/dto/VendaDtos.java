package br.com.heracles.heracles_api.operacoes.dto;

import br.com.heracles.heracles_api.operacoes.domain.ItemVenda;
import br.com.heracles.heracles_api.operacoes.domain.MetodoPagamento;
import br.com.heracles.heracles_api.operacoes.domain.Venda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class VendaDtos {

    private VendaDtos() {
    }

    /**
     * Fechamento de venda.
     *
     * O corpo traz apenas produto e quantidade. Preco unitario e valor
     * total sao calculados no servidor a partir da tabela de precos — se
     * viessem do cliente, bastaria editar a requisicao para levar o
     * suplemento por um centavo. Pela mesma razao o operador nao e um
     * campo: vem do token.
     */
    public record Registrar(
            @NotNull(message = "Informe a unidade da venda")
            Long unidadeId,

            /** Opcional: venda de balcao para visitante nao tem aluno. */
            Long alunoId,

            @NotNull(message = "Informe a forma de pagamento")
            MetodoPagamento metodoPagamento,

            @NotNull(message = "Informe os itens da venda")
            @Size(min = 1, message = "A venda precisa de pelo menos um item")
            @Size(max = 100, message = "Venda com itens demais")
            List<@Valid Item> itens
    ) {
        public record Item(
                @NotNull(message = "Informe o produto")
                Long produtoId,

                @NotNull(message = "Informe a quantidade")
                @Min(value = 1, message = "A quantidade precisa ser de pelo menos 1")
                @Max(value = 1000, message = "Quantidade acima do limite por item")
                Integer quantidade
        ) {
        }
    }

    public record Response(
            Long id,
            Long unidadeId,
            String unidadeNome,
            String operadorNome,
            Long alunoId,
            String alunoNome,
            BigDecimal valorTotal,
            LocalDateTime dataVenda,
            MetodoPagamento metodoPagamento,
            List<ItemResponse> itens
    ) {
        public static Response de(Venda venda) {
            return new Response(
                    venda.getId(),
                    venda.getUnidade().getId(),
                    venda.getUnidade().getNome(),
                    venda.getOperador().getNome(),
                    venda.getAluno() != null ? venda.getAluno().getId() : null,
                    venda.getAluno() != null ? venda.getAluno().getNome() : null,
                    venda.getValorTotal(),
                    venda.getDataVenda(),
                    venda.getMetodoPagamento(),
                    venda.getItens().stream().map(ItemResponse::de).toList()
            );
        }

        public record ItemResponse(
                Long produtoId,
                String produtoNome,
                Integer quantidade,
                BigDecimal precoUnitario,
                BigDecimal subtotal
        ) {
            public static ItemResponse de(ItemVenda item) {
                return new ItemResponse(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.subtotal()
                );
            }
        }
    }
}
