package br.com.heracles.heracles_api.core.dto;

import java.math.BigDecimal;

/**
 * Tempo medio de permanencia numa ficha antes da troca, por nivel
 * (iniciante/intermediario/avancado), entre periodos encerrados no
 * periodo consultado.
 */
public record LinhaPermanenciaPorNivel(String nivel, long quantidade, BigDecimal diasMedios) {
}
