package br.com.heracles.heracles_api.matriculas.dto;

/**
 * Um aluno e uma unidade vigente que o plano dele cobre. Um aluno de plano
 * de rede aparece mais de uma vez, uma por unidade coberta — mesmo
 * espalhamento usado em ContagemAgrupada para relatorios por unidade.
 */
public record AlunoUnidade(Long alunoId, Long unidadeId, String unidadeNome) {
}
