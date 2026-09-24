package br.com.heracles.heracles_api.operacoes.dto;

import java.math.BigDecimal;

/**
 * Projecao da consulta agregada: faturamento e numero de vendas no periodo,
 * separado entre venda de aluno matriculado e venda avulsa (visitante).
 */
public record LinhaFaturamentoPorTipoCliente(boolean vendaParaAluno, BigDecimal faturamentoTotal, long quantidadeVendas) {
}
