package br.com.heracles.heracles_api.core.dto;

import java.math.BigDecimal;

/**
 * Cobertura de anamnese por unidade: entre alunos com matricula vigente,
 * quantos ja preencheram a anamnese, em percentual — do pior pro melhor.
 *
 * A unidade vem da assinatura vigente de cada aluno: um aluno de plano de
 * rede conta uma vez em cada unidade que o plano cobre, mesmo espalhamento
 * de LinhaEvolucaoFisicaPorUnidade.
 */
public record LinhaCoberturaAnamnesePorUnidade(
        String unidadeNome, long quantidadeAlunos, long quantidadeComAnamnese, BigDecimal percentualCobertura) {
}
