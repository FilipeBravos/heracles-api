package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "O e-mail e obrigatorio")
            @Email(message = "Informe um e-mail valido")
            String email,

            @NotBlank(message = "A senha e obrigatoria")
            String senha
    ) {
    }

    /** Token de acesso e o minimo de identidade que a interface precisa para se desenhar. */
    public record LoginResponse(
            String token,
            String tipo,
            long expiraEmSegundos,
            UsuarioAutenticado usuario
    ) {
    }

    public record UsuarioAutenticado(
            Long id,
            String nome,
            String email,
            TipoPerfil tipoPerfil
    ) {
    }
}
