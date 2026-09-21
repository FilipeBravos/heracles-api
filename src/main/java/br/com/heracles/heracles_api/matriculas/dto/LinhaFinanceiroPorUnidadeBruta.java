package br.com.heracles.heracles_api.matriculas.dto;

import java.math.BigDecimal;

/**
 * Projecao do mrr e das assinaturas ativas agrupados por unidade — o
 * ticket medio ainda nao existe aqui porque divisao nao entra em `select
 * new`, so o servico calcula em cima desta linha.
 */
public record LinhaFinanceiroPorUnidadeBruta(Long unidadeId, String unidadeNome, BigDecimal mrr, long assinaturasAtivas) {
}
