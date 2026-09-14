package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Projecao de leitura de um usuario.
 *
 * Nao existe campo de senha aqui, e essa e a unica forma pela qual um
 * usuario deixa a API: o hash deixou de ser alcancavel por serializacao.
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        TipoPerfil tipoPerfil,
        StatusUsuario status,
        LocalDateTime dataCadastro,
        List<TreinoResumoResponse> treinos
) {

    /** Usa a colecao de treinos; exige que ela tenha sido carregada (@EntityGraph). */
    public static UsuarioResponse comTreinos(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getTipoPerfil(),
                usuario.getStatus(),
                usuario.getDataCadastro(),
                usuario.getTreinos().stream().map(TreinoResumoResponse::de).toList()
        );
    }

    /** Nao toca na colecao de treinos: seguro para respostas de escrita. */
    public static UsuarioResponse semTreinos(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getTipoPerfil(),
                usuario.getStatus(),
                usuario.getDataCadastro(),
                List.of()
        );
    }
}
