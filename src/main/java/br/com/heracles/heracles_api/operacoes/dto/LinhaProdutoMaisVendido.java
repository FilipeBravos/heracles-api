package br.com.heracles.heracles_api.operacoes.dto;

import java.math.BigDecimal;

/** Projecao da consulta agregada: quanto um produto vendeu no periodo, em quantidade e em receita. */
public record LinhaProdutoMaisVendido(Long produtoId, String produtoNome, long quantidadeVendida, BigDecimal receitaTotal) {
}
