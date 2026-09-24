package br.com.heracles.heracles_api.core.dto;

import java.time.LocalDateTime;

/** Projecao de um periodo de ficha ja encerrado, com o suficiente pra calcular a permanencia em Java. */
public record LinhaHistoricoParaPermanencia(String treinoNivel, LocalDateTime vinculadoEm, LocalDateTime desvinculadoEm) {
}
