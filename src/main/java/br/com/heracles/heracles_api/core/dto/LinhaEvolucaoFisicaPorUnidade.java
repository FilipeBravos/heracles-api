package br.com.heracles.heracles_api.core.dto;

import java.math.BigDecimal;

/**
 * Evolucao fisica media por unidade: a media do delta (ultima avaliacao
 * menos a primeira, no periodo) entre alunos com pelo menos duas
 * avaliacoes. Um delta individual ausente (falta peso ou altura numa das
 * pontas) fica de fora so daquela media, sem zerar o aluno inteiro.
 *
 * A unidade vem da assinatura vigente de cada aluno: um aluno de plano de
 * rede conta a propria evolucao em cada unidade que o plano cobre — mesmo
 * espalhamento de AssinaturaRepository.contarAtivasPorUnidadeEm.
 */
public record LinhaEvolucaoFisicaPorUnidade(
        String unidadeNome, long quantidadeAlunos,
        BigDecimal deltaPesoMedio, BigDecimal deltaPercentualGorduraMedio, BigDecimal deltaImcMedio) {
}
