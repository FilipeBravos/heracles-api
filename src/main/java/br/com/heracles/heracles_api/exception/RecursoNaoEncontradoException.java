package br.com.heracles.heracles_api.exception;

/** Lancada quando um identificador valido nao corresponde a nenhum registro. Vira 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException("%s nao encontrado(a) para o id %s".formatted(recurso, id));
    }
}
