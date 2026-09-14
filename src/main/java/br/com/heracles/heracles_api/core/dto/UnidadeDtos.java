package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoUnidade;
import br.com.heracles.heracles_api.core.domain.Unidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UnidadeDtos {

    private UnidadeDtos() {
    }

    public record Request(
            @NotBlank(message = "O nome da unidade e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @NotNull(message = "O tipo da unidade e obrigatorio")
            TipoUnidade tipo,

            @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
            String endereco,

            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String telefone
    ) {
    }

    public record Response(
            Long id,
            String nome,
            TipoUnidade tipo,
            String endereco,
            String telefone
    ) {
        public static Response de(Unidade unidade) {
            return new Response(
                    unidade.getId(),
                    unidade.getNome(),
                    unidade.getTipo(),
                    unidade.getEndereco(),
                    unidade.getTelefone()
            );
        }
    }
}
