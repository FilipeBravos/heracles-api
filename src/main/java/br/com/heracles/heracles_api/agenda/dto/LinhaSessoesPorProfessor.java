package br.com.heracles.heracles_api.agenda.dto;

/** Projecao da consulta agregada: quantas sessoes de personal um professor realizou no periodo. */
public record LinhaSessoesPorProfessor(Long professorId, String professorNome, long quantidadeSessoes) {
}
