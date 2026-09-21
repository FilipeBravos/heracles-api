package br.com.heracles.heracles_api.matriculas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projecao da consulta: uma matricula ativa e o instante do ultimo
 * check-in liberado do aluno — nulo quando ele nunca fez um. Os dias
 * parado sao calculados no servico, nao aqui.
 */
public record LinhaAlunoInativoBruto(
        Long assinaturaId, Long alunoId, String alunoNome, String planoNome,
        LocalDate dataVencimento, LocalDateTime ultimoCheckin) {
}
