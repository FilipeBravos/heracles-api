package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.CanalLembrete;
import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;

import java.time.LocalDateTime;

/** Projecao de um lembrete enviado, com o suficiente pra avaliar a conversao em Java. */
public record LinhaLembreteParaEfetividade(
        Long assinaturaId, EstagioLembrete estagio, CanalLembrete canal, LocalDateTime dataEnvio) {
}
