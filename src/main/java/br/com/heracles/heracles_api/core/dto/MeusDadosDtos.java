package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que o proprio usuario le e edita de si mesmo, fora da ficha e da
 * matricula: nome, telefone e senha.
 *
 * Deliberadamente menor que UsuarioRequests.Atualizar: e-mail e CPF sao
 * identidade, nao contato, e ficam de fora da autoedicao. Trocar o
 * proprio e-mail mudaria o que o login usa para achar a conta; o CPF e
 * documento, nao dado que se corrige na conta.
 */
public final class MeusDadosDtos {

    private MeusDadosDtos() {
    }

    public record Response(String nome, String email, String telefone) {
        public static Response de(Usuario usuario) {
            return new Response(usuario.getNome(), usuario.getEmail(), usuario.getTelefone());
        }
    }

    public record Atualizar(
            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String telefone
    ) {
    }

    /**
     * A senha atual e exigida mesmo com o token ja autenticado: sem ela,
     * um token vazado (por exemplo por XSS) bastaria para trocar a senha
     * e trancar o dono de fora — a diferenca entre ler algo que e do
     * portador do token e assumir a conta.
     */
    public record TrocarSenha(
            @NotBlank(message = "Informe a senha atual")
            String senhaAtual,

            @NotBlank(message = "A nova senha e obrigatoria")
            @Size(min = 8, message = "A nova senha deve ter no minimo 8 caracteres")
            String novaSenha
    ) {
    }
}
