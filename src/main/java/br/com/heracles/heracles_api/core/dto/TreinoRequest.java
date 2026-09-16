package br.com.heracles.heracles_api.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Contrato de entrada para criacao e edicao de uma ficha de treino. */
public record TreinoRequest(

        @NotBlank(message = "O nome da ficha e obrigatorio")
        @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
        String nome,

        @NotBlank(message = "O foco e obrigatorio")
        @Size(max = 50, message = "O foco deve ter no maximo 50 caracteres")
        String foco,

        @NotBlank(message = "O nivel e obrigatorio")
        @Size(max = 50, message = "O nivel deve ter no maximo 50 caracteres")
        String nivel,

        @NotNull(message = "Informe a lista de exercicios")
        @Size(min = 1, message = "A ficha precisa de pelo menos um exercicio")
        List<@Valid ExercicioRequest> exercicios
) {

    public record ExercicioRequest(
            /* Id do exercicio existente, ou nulo para um exercicio novo.
               O servico reconcilia por esse id em vez de apagar e recriar. */
            Long id,

            @NotBlank(message = "O nome do exercicio e obrigatorio")
            @Size(max = 100, message = "O nome do exercicio deve ter no maximo 100 caracteres")
            String nome,

            @NotNull(message = "Informe o numero de series")
            @Min(value = 1, message = "A ficha precisa de pelo menos 1 serie")
            @Max(value = 20, message = "20 series e o limite por exercicio")
            Integer series,

            @NotNull(message = "Informe as repeticoes")
            @Min(value = 1, message = "As repeticoes comecam em 1")
            @Max(value = 500, message = "500 repeticoes e o limite por serie")
            Integer repeticoesMin,

            @NotNull(message = "Informe as repeticoes")
            @Min(value = 1, message = "As repeticoes comecam em 1")
            @Max(value = 500, message = "500 repeticoes e o limite por serie")
            Integer repeticoesMax,

            @Size(max = 50, message = "A carga deve ter no maximo 50 caracteres")
            String carga,

            String observacoes
    ) {

        /**
         * Faixa coerente: o limite superior nao pode ser menor que o inferior.
         *
         * Vive aqui, e nao no controller, para que a mensagem chegue ao
         * formulario junto com os demais erros de campo. Quando um dos lados
         * e nulo, quem reporta e o @NotNull correspondente.
         */
        @AssertTrue(message = "O maximo de repeticoes nao pode ser menor que o minimo")
        public boolean isFaixaDeRepeticoesCoerente() {
            if (repeticoesMin == null || repeticoesMax == null) {
                return true;
            }
            return repeticoesMax >= repeticoesMin;
        }
    }
}
