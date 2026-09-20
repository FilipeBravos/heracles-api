package br.com.heracles.heracles_api.matriculas.dto;

/**
 * Projecao da consulta agregada: quantas assinaturas caem num grupo (plano
 * ou unidade). Um so tipo para os dois agrupamentos — a forma e identica,
 * so a coluna de origem muda.
 */
public record ContagemAgrupada(Long id, String nome, Long quantidade) {
}
