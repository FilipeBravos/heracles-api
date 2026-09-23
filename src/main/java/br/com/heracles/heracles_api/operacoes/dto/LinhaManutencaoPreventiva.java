package br.com.heracles.heracles_api.operacoes.dto;

import java.time.LocalDate;

/**
 * Um equipamento com manutencao preventiva vencida ou vencendo.
 *
 * ultimaManutencao e a data de resolucao do ultimo chamado, ou o cadastro
 * do equipamento se ele nunca teve nenhum chamado resolvido ainda.
 */
public record LinhaManutencaoPreventiva(
        Long equipamentoId, String equipamentoNome, Long unidadeId, String unidadeNome,
        Integer intervaloDiasManutencao, LocalDate ultimaManutencao, LocalDate proximaManutencaoDevida,
        long diasAtraso) {
}
