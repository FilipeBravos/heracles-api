package br.com.heracles.heracles_api.core.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Uma linha do alerta de reavaliacao vencida: matricula ativa, mas a
 * ultima avaliacao fisica passou da janela — ou nunca aconteceu.
 * `diasSemAvaliacao` nulo significa "nunca fez uma", o caso mais grave,
 * nao "zero dias".
 */
public record LinhaReavaliacaoVencida(
        Long alunoId, String alunoNome, String email, String telefone,
        LocalDate ultimaAvaliacao, Long diasSemAvaliacao
) {
    public static LinhaReavaliacaoVencida de(LinhaReavaliacaoVencidaBruta bruta, LocalDate hoje) {
        Long dias = bruta.ultimaAvaliacao() != null
                ? ChronoUnit.DAYS.between(bruta.ultimaAvaliacao(), hoje)
                : null;
        return new LinhaReavaliacaoVencida(
                bruta.alunoId(), bruta.alunoNome(), bruta.email(), bruta.telefone(),
                bruta.ultimaAvaliacao(), dias);
    }
}
