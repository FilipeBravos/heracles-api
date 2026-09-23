package br.com.heracles.heracles_api.operacoes.dto;

import java.time.LocalDateTime;

/** Projecao interna: a data de resolucao mais recente de um equipamento, para a manutencao preventiva. */
public record LinhaUltimaManutencao(Long equipamentoId, LocalDateTime ultimaResolucao) {
}
