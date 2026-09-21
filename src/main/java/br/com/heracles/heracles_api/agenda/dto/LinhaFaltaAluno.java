package br.com.heracles.heracles_api.agenda.dto;

/** Projecao da consulta agregada: faltas e presencas confirmadas de um aluno no periodo, do que mais falta pro que menos. */
public record LinhaFaltaAluno(Long alunoId, String alunoNome, long faltas, long presencas) {
}
