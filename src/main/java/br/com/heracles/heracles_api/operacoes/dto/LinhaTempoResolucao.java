package br.com.heracles.heracles_api.operacoes.dto;

import java.time.LocalDateTime;

/**
 * Projecao da consulta: quando um chamado resolvido foi aberto e quando
 * foi resolvido — so os dois instantes, pro tempo medio de resolucao ser
 * calculado no servico (duracao entre timestamps nao e coisa pra JPQL).
 */
public record LinhaTempoResolucao(LocalDateTime dataChamado, LocalDateTime dataResolucao) {
}
