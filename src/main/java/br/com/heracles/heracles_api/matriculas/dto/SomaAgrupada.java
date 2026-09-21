package br.com.heracles.heracles_api.matriculas.dto;

import java.math.BigDecimal;

/**
 * Projecao da consulta agregada: quanto dinheiro cai num grupo (plano ou
 * unidade). Irma de ContagemAgrupada, so que somando um valor em vez de
 * contar linhas.
 */
public record SomaAgrupada(Long id, String nome, BigDecimal valor) {
}
