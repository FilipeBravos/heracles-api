package br.com.heracles.heracles_api.matriculas.domain;

public enum StatusAssinatura {

    /** Em dia. */
    ATIVA,

    /** Matricula do aluno, em atraso de pagamento — continua vigente. */
    INADIMPLENTE,

    /** Encerrada. Libera o aluno para se rematricular. */
    CANCELADA;

    public boolean estaVigente() {
        return this != CANCELADA;
    }
}
