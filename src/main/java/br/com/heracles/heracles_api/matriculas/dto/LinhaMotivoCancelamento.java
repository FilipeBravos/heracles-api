package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.MotivoCancelamento;

/** Projecao da consulta agregada: quantos cancelamentos tiveram este motivo. */
public record LinhaMotivoCancelamento(MotivoCancelamento motivo, Long quantidade) {
}
