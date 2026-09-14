package br.com.heracles.heracles_api.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

            @NotBlank(message = "As repeticoes sao obrigatorias")
            @Size(max = 50, message = "As repeticoes devem ter no maximo 50 caracteres")
            String repeticoes,

            String observacoes
    ) {
    }
}
