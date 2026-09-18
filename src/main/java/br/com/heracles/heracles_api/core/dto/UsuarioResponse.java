package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Projecao de leitura de um usuario.
 *
 * Nao existe campo de senha aqui, e essa e a unica forma pela qual um
 * usuario deixa a API: o hash deixou de ser alcancavel por serializacao.
 *
 * A foto em si nao vai aqui — `temFoto` so diz se ha uma. Embutir a
 * imagem em base64 numa listagem paginada de vinte alunos multiplicaria
 * o tamanho da resposta por nada que a tabela usa; quem precisa dos
 * bytes pede GET /usuarios/{id}/foto.
 *
 * `anamnesePreenchida` vem calculado por quem monta a resposta (o
 * servico, que ja consulta AnamneseRepository), nao lido daqui — o DTO
 * fica sem tocar em repositorio.
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        String endereco,
        String cep,
        LocalDate dataNascimento,
        boolean temFoto,
        Long planoEscolhidoId,
        String planoEscolhidoNome,
        boolean anamnesePreenchida,
        TipoPerfil tipoPerfil,
        StatusUsuario status,
        LocalDateTime dataCadastro,
        List<TreinoResumoResponse> treinos
) {

    /** Usa a colecao de treinos; exige que ela tenha sido carregada (@EntityGraph). */
    public static UsuarioResponse comTreinos(Usuario usuario, boolean anamnesePreenchida) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getEndereco(),
                usuario.getCep(),
                usuario.getDataNascimento(),
                usuario.getFoto() != null,
                usuario.getPlanoEscolhido() != null ? usuario.getPlanoEscolhido().getId() : null,
                usuario.getPlanoEscolhido() != null ? usuario.getPlanoEscolhido().getNome() : null,
                anamnesePreenchida,
                usuario.getTipoPerfil(),
                usuario.getStatus(),
                usuario.getDataCadastro(),
                usuario.getTreinos().stream().map(TreinoResumoResponse::de).toList()
        );
    }

    /** Nao toca na colecao de treinos: seguro para respostas de escrita. */
    public static UsuarioResponse semTreinos(Usuario usuario, boolean anamnesePreenchida) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getEndereco(),
                usuario.getCep(),
                usuario.getDataNascimento(),
                usuario.getFoto() != null,
                usuario.getPlanoEscolhido() != null ? usuario.getPlanoEscolhido().getId() : null,
                usuario.getPlanoEscolhido() != null ? usuario.getPlanoEscolhido().getNome() : null,
                anamnesePreenchida,
                usuario.getTipoPerfil(),
                usuario.getStatus(),
                usuario.getDataCadastro(),
                List.of()
        );
    }
}
