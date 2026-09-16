package br.com.heracles.heracles_api.matriculas.dto;

/**
 * Projecao da consulta agregada: quantas matriculas comecaram num mes.
 *
 * Classe propria, e nao record aninhado, porque o JPQL referencia o tipo
 * pelo nome completo na expressao de construtor.
 */
public record ContagemMensal(Integer ano, Integer mes, Long quantidade) {
}
