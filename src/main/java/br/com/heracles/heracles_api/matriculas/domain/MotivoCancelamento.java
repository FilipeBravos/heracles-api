package br.com.heracles.heracles_api.matriculas.domain;

/**
 * Por que o aluno cancelou — a pergunta que a secretaria faz no balcao
 * na hora de cancelar, nao uma pesquisa enviada depois. Quem ja saiu da
 * academia dificilmente volta ao sistema para responder sozinho.
 */
public enum MotivoCancelamento {
    PRECO,
    MUDANCA,
    INSATISFACAO,
    FALTA_TEMPO,
    SAUDE,
    CONCORRENCIA,
    OUTRO
}
