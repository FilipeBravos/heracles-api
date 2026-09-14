package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import jakarta.validation.constraints.*;

/**
 * Contratos de entrada para usuarios.
 *
 * A entidade Usuario nao e mais ligada diretamente ao corpo da requisicao:
 * o cliente so consegue enviar os campos declarados aqui. Em particular
 * "id", "status", "dataCadastro" e "senhaHash" nao sao enderecaveis, o que
 * elimina tanto a escalacao de privilegio quanto a sobrescrita de registro
 * por id enviado de fora.
 */
public final class UsuarioRequests {

    private UsuarioRequests() {
    }

    public record Criar(
            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @NotBlank(message = "O CPF e obrigatorio")
            @Pattern(
                    regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                    message = "Informe um CPF valido, com ou sem pontuacao"
            )
            String cpf,

            @NotBlank(message = "O e-mail e obrigatorio")
            @Email(message = "Informe um e-mail valido")
            @Size(max = 100, message = "O e-mail deve ter no maximo 100 caracteres")
            String email,

            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String telefone,

            @NotNull(message = "O perfil e obrigatorio")
            TipoPerfil tipoPerfil,

            @NotBlank(message = "A senha e obrigatoria")
            @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres")
            String senha
    ) {
    }

    public record Atualizar(
            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @NotBlank(message = "O CPF e obrigatorio")
            @Pattern(
                    regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                    message = "Informe um CPF valido, com ou sem pontuacao"
            )
            String cpf,

            @NotBlank(message = "O e-mail e obrigatorio")
            @Email(message = "Informe um e-mail valido")
            @Size(max = 100, message = "O e-mail deve ter no maximo 100 caracteres")
            String email,

            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String telefone
    ) {
    }

    /** Sincroniza a lista completa de fichas de um aluno. Lista vazia desvincula todas. */
    public record VincularTreinos(
            @NotNull(message = "Informe a lista de treinos")
            java.util.List<@NotNull Long> treinosIds
    ) {
    }
}
