package br.com.heracles.heracles_api.matriculas.domain;

/**
 * O estagio da regua de cobranca que disparou o lembrete — os mesmos tres
 * que o relatorio de inadimplencia ja usa. Um lembrete por estagio, nunca
 * repetido: virar de estagio e o que libera um lembrete novo.
 */
public enum EstagioLembrete {
    VENCE_EM_BREVE,
    VENCIDA,
    INADIMPLENTE
}
