package br.com.heracles.heracles_api.operacoes.dto;

import java.time.LocalDate;

/**
 * Um produto ativo sem venda ha pelo menos o limiar configurado, do mais
 * parado pro menos.
 *
 * ultimaVenda e nula quando o produto nunca vendeu — nesse caso diasParado
 * conta a partir do cadastro, nao de uma venda que nunca aconteceu.
 */
public record LinhaProdutoParado(
        Long produtoId, String produtoNome, String marca, Long unidadeId, String unidadeNome,
        LocalDate ultimaVenda, long diasParado) {
}
