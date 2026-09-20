package br.com.heracles.heracles_api.matriculas.dto;

/** Projecao da consulta agregada: quantos check-ins liberados numa unidade, numa hora do dia. */
public record LinhaOcupacao(Long unidadeId, String unidadeNome, int hora, long quantidade) {
}
