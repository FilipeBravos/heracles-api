package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.MotivoAcesso;

/** Projecao da consulta agregada: quantas tentativas de acesso barradas numa unidade, por motivo, no periodo. */
public record LinhaMotivoAcessoNegado(Long unidadeId, String unidadeNome, MotivoAcesso motivo, long quantidade) {
}
