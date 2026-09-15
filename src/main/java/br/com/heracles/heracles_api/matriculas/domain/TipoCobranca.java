package br.com.heracles.heracles_api.matriculas.domain;

/**
 * Como o plano e cobrado — e, por consequencia, de quanto em quanto tempo
 * a assinatura vence.
 *
 * Nao e so um rotulo: e o periodo que a renovacao avanca. Sem isso, a
 * secretaria teria de digitar a data de vencimento a mao a cada mes, que
 * e exatamente onde entra o erro de um ano de acesso por engano.
 */
public enum TipoCobranca {

    RECORRENTE(1),
    PACOTE_ANUAL(12);

    private final int mesesDeVigencia;

    TipoCobranca(int mesesDeVigencia) {
        this.mesesDeVigencia = mesesDeVigencia;
    }

    public int getMesesDeVigencia() {
        return mesesDeVigencia;
    }
}
