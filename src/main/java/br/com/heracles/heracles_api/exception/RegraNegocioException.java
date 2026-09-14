package br.com.heracles.heracles_api.exception;

/** Lancada quando a requisicao e bem formada mas conflita com o estado atual. Vira 409. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
