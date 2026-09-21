package br.com.heracles.heracles_api.operacoes.dto;

import java.math.BigDecimal;

/** Projecao da consulta agregada: quantos chamados e quanto custou um equipamento no periodo. */
public record LinhaEquipamentoProblematico(
        Long equipamentoId, String equipamentoNome, String unidadeNome, long quantidadeChamados, BigDecimal custoTotal) {
}
