package br.com.heracles.heracles_api.core.dto;

/**
 * Numeros da tela inicial.
 *
 * Antes eram literais no componente Angular ("128 alunos ativos"), que
 * apareciam identicos com tres ou tres mil alunos na base.
 */
public record DashboardResumoResponse(
        long alunosAtivos,
        long alunosInativos,
        long treinosCadastrados,
        long fichasAtribuidas,
        long novasMatriculasNoMes
) {
}
