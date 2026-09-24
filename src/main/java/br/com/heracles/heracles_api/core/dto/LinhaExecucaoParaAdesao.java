package br.com.heracles.heracles_api.core.dto;

/** Projecao de uma execucao de exercicio, com a prescricao do exercicio ainda vinculado, pra calcular adesao em Java. */
public record LinhaExecucaoParaAdesao(
        Long alunoId, String alunoNome, Integer series, Integer repeticoesMin,
        Integer seriesRealizadas, Integer repeticoesRealizadas) {
}
