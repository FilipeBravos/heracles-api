package br.com.heracles.heracles_api.matriculas.domain;

/**
 * Por que o acesso foi liberado ou barrado.
 *
 * Vive no dominio, nao no DTO: e o mesmo motivo que o veredito da
 * catraca (AssinaturaDtos.Acesso) devolve na hora e que cada check-in
 * grava no historico de frequencia — as duas leituras precisam ser o
 * mesmo valor, nao duas copias que podem divergir.
 */
public enum MotivoAcesso {
    LIBERADO,
    SEM_MATRICULA,
    INADIMPLENTE,
    VENCIDA,
    UNIDADE_NAO_COBERTA
}
