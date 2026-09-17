package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O que o aluno ve de si mesmo.
 *
 * Todo metodo aqui recebe o e-mail do token, nunca um id vindo da
 * requisicao. E a diferenca entre "minhas fichas" e "as fichas de quem
 * eu disser": com um id no caminho, bastaria trocar o numero para ler a
 * ficha de outro aluno.
 */
@Service
public class MinhaAreaService {

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;

    public MinhaAreaService(UsuarioRepository usuarioRepository, TreinoRepository treinoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
    }

    @Transactional(readOnly = true)
    public List<TreinoResponse> minhasFichas(String emailAutenticado) {
        Usuario eu = usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));

        return treinoRepository.fichasDoAluno(eu.getId()).stream()
                .map(TreinoResponse::de)
                .toList();
    }
}
