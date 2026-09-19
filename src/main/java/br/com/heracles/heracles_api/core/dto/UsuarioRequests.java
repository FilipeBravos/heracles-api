package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

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

    /**
     * Endereco, CEP, data de nascimento e plano escolhido nao levam
     * @NotBlank/@NotNull aqui: este contrato serve para os quatro perfis, e
     * so o aluno usa esses campos de fato — professor, secretaria e admin
     * nao tem "plano escolhido". A obrigatoriedade para aluno e regra de
     * negocio, verificada em UsuarioService.criar, nao forma do corpo.
     */
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

            @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
            String endereco,

            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "Informe um CEP valido, com ou sem hifen")
            String cep,

            @Past(message = "A data de nascimento precisa ser no passado")
            LocalDate dataNascimento,

            /** Base64 puro, sem o prefixo "data:image/...;base64,". Opcional mesmo para aluno. */
            String fotoBase64,

            @Pattern(regexp = "image/(jpeg|png|webp)", message = "A foto precisa ser JPEG, PNG ou WebP")
            String fotoContentType,

            Long planoEscolhidoId,

            @NotNull(message = "O perfil e obrigatorio")
            TipoPerfil tipoPerfil,

            @NotBlank(message = "A senha e obrigatoria")
            @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres")
            String senha,

            /**
             * O "clique para assinar" do contrato de adesao: nome digitado
             * mais aceite. Obrigatorio so para aluno — regra de negocio em
             * UsuarioService.criar, mesmo motivo de endereco/cep/plano.
             */
            @Size(max = 100, message = "O nome no contrato deve ter no maximo 100 caracteres")
            String nomeAssinaturaContrato,

            Boolean aceiteContrato
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
            String telefone,

            @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
            String endereco,

            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "Informe um CEP valido, com ou sem hifen")
            String cep,

            @Past(message = "A data de nascimento precisa ser no passado")
            LocalDate dataNascimento,

            String fotoBase64,

            @Pattern(regexp = "image/(jpeg|png|webp)", message = "A foto precisa ser JPEG, PNG ou WebP")
            String fotoContentType,

            Long planoEscolhidoId
    ) {
    }

    /** Sincroniza a lista completa de fichas de um aluno. Lista vazia desvincula todas. */
    public record VincularTreinos(
            @NotNull(message = "Informe a lista de treinos")
            java.util.List<@NotNull Long> treinosIds
    ) {
    }
}
