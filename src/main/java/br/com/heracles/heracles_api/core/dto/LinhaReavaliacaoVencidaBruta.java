package br.com.heracles.heracles_api.core.dto;

import java.time.LocalDate;

/**
 * Projecao da consulta: um aluno com matricula ativa e a data da sua
 * ultima avaliacao fisica — nula quando ele nunca fez uma. Os dias sem
 * avaliacao sao calculados no servico, nao aqui.
 */
public record LinhaReavaliacaoVencidaBruta(
        Long alunoId, String alunoNome, String email, String telefone, LocalDate ultimaAvaliacao) {
}
