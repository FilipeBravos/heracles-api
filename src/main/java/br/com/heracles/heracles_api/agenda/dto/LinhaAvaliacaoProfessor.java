package br.com.heracles.heracles_api.agenda.dto;

/** Projecao da consulta agregada: a nota media de um professor, do melhor pro pior. */
public record LinhaAvaliacaoProfessor(Long professorId, String professorNome, double notaMedia, long quantidade) {
}
