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
        /**
         * Matriculas de fato iniciadas no mes.
         *
         * Antes era a contagem de cadastros de usuario — um numero que
         * subia ao cadastrar um professor e nao subia ao matricular um
         * aluno ja cadastrado. Com o schema matriculas em uso, passa a
         * contar assinaturas.
         */
        long novasMatriculasNoMes,
        /** Matriculas em atraso de pagamento — a fila de cobranca. */
        long matriculasInadimplentes,
        /** Vigentes que ja passaram do vencimento: o acesso caiu hoje. */
        long matriculasVencidas,
        /** Aparelhos fora de operacao agora — o numero que pede acao no dia. */
        long equipamentosEmManutencao,
        long produtosComEstoqueBaixo,
        long vendasNoMes,
        java.math.BigDecimal faturamentoDoMes
) {
}
