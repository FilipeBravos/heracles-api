package br.com.heracles.heracles_api.operacoes.dto;

import java.math.BigDecimal;

/** Projecao da consulta agregada: quantos chamados e quanto custou a manutencao de uma unidade no periodo. */
public record LinhaManutencaoPorUnidade(Long unidadeId, String unidadeNome, long quantidadeChamados, BigDecimal custoTotal) {
}
