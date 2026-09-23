package br.com.heracles.heracles_api.operacoes.dto;

import br.com.heracles.heracles_api.operacoes.domain.MetodoPagamento;

import java.math.BigDecimal;

/** Projecao da consulta agregada: faturamento e numero de vendas de uma unidade por forma de pagamento no periodo. */
public record LinhaFaturamentoPorMetodoPagamento(
        Long unidadeId, String unidadeNome, MetodoPagamento metodoPagamento,
        BigDecimal faturamentoTotal, long quantidadeVendas) {
}
