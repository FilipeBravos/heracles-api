package br.com.heracles.heracles_api.operacoes.dto;

import java.math.BigDecimal;

/** Projecao da consulta agregada: faturamento e numero de vendas de uma unidade no periodo. */
public record LinhaFaturamentoPorUnidade(Long unidadeId, String unidadeNome, BigDecimal faturamentoTotal, long quantidadeVendas) {
}
