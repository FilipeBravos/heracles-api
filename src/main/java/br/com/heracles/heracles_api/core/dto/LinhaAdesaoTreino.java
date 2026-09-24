package br.com.heracles.heracles_api.core.dto;

import java.math.BigDecimal;

/**
 * Adesao ao treino de um aluno no periodo: volume prescrito (series vezes
 * repeticoes minimas do exercicio) contra volume realizado (series vezes
 * repeticoes que o aluno de fato fez), do pior pro melhor.
 *
 * So entram execucoes cujo exercicio ainda existe na ficha de origem —
 * sem a prescricao vigente, nao ha volume prescrito pra comparar.
 */
public record LinhaAdesaoTreino(
        Long alunoId, String alunoNome, long quantidadeExecucoes,
        long volumePrescritoTotal, long volumeRealizadoTotal, BigDecimal taxaAdesao) {
}
