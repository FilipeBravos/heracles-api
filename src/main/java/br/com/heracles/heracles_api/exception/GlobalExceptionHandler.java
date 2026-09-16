package br.com.heracles.heracles_api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traducao unica de excecao para resposta HTTP.
 *
 * Antes, "aluno nao encontrado" chegava ao cliente como 500, indistinguivel
 * de uma falha real de servidor. Todas as respostas de erro agora seguem o
 * formato ProblemDetail (RFC 9457), entao o front tem uma forma estavel para
 * renderizar mensagens.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), "recurso-nao-encontrado");
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito com o estado atual", ex.getMessage(), "regra-de-negocio");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(BadCredentialsException ex) {
        // Mensagem deliberadamente generica: nao revela se o e-mail existe.
        return problema(HttpStatus.UNAUTHORIZED, "Credenciais invalidas",
                "E-mail ou senha incorretos.", "credenciais-invalidas");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail tratarAcessoNegado(AuthorizationDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil nao tem permissao para esta operacao.", "acesso-negado");
    }

    /**
     * Erros de bean validation, campo a campo, para que o formulario consiga
     * destacar exatamente o que precisa ser corrigido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));

        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Um ou mais campos nao passaram na validacao.", "validacao");
        detalhe.setProperty("erros", erros);
        return detalhe;
    }

    /**
     * Violacao em parametro de rota ou de query (@Min/@Max nos controllers).
     *
     * Sem este tratamento, `?dias=9999` virava 500 — indistinguivel de uma
     * falha real de servidor, quando o problema esta na requisicao.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail tratarParametroInvalido(ConstraintViolationException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violacao ->
                erros.putIfAbsent(nomeDoParametro(violacao), violacao.getMessage()));

        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Parametro invalido",
                "Um ou mais parametros da requisicao estao fora do permitido.", "validacao");
        detalhe.setProperty("erros", erros);
        return detalhe;
    }

    /** O path vem como "metodo.parametro"; ao cliente interessa so o parametro. */
    private String nomeDoParametro(ConstraintViolation<?> violacao) {
        String caminho = violacao.getPropertyPath().toString();
        int ultimoPonto = caminho.lastIndexOf('.');
        return ultimoPonto >= 0 ? caminho.substring(ultimoPonto + 1) : caminho;
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String tipo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://heracles.com.br/erros/" + tipo));
        return problema;
    }
}
