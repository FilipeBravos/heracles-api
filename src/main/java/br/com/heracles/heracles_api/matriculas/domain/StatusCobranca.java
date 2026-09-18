package br.com.heracles.heracles_api.matriculas.domain;

public enum StatusCobranca {

    /** Gerada, aguardando o aluno pagar. */
    PENDENTE,

    /** Paga — foi ela que empurrou o vencimento da assinatura. */
    PAGA,

    /** A assinatura foi cancelada antes do pagamento entrar. */
    CANCELADA
}
