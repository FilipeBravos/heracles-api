package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de cadastro de usuarios.
 *
 * Cada operacao de escrita e uma transacao: ler-e-salvar deixou de ser duas
 * transacoes independentes, o que abria corrida entre dois atendentes
 * editando o mesmo aluno.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final TreinoRepository treinoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          TreinoRepository treinoRepository,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.treinoRepository = treinoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return repository.buscarPaginadoComTreinos(pageable).map(UsuarioResponse::comTreinos);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.comTreinos(carregarComTreinos(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequests.Criar request) {
        String cpf = normalizarCpf(request.cpf());

        if (repository.existsByCpf(cpf)) {
            throw new RegraNegocioException("Ja existe um cadastro com o CPF informado.");
        }
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraNegocioException("Ja existe um cadastro com o e-mail informado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setCpf(cpf);
        usuario.setEmail(request.email());
        usuario.setTelefone(request.telefone());
        usuario.setTipoPerfil(request.tipoPerfil());
        // A senha e cifrada aqui, no servidor. O cliente nunca envia nem recebe hash.
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));

        return UsuarioResponse.semTreinos(repository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequests.Atualizar request) {
        Usuario usuario = carregarComTreinos(id);
        String cpf = normalizarCpf(request.cpf());

        if (repository.existsByCpfAndIdNot(cpf, id)) {
            throw new RegraNegocioException("Ja existe outro cadastro com o CPF informado.");
        }
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new RegraNegocioException("Ja existe outro cadastro com o e-mail informado.");
        }

        usuario.setNome(request.nome());
        usuario.setCpf(cpf);
        usuario.setEmail(request.email());
        usuario.setTelefone(request.telefone());

        return UsuarioResponse.comTreinos(usuario);
    }

    @Transactional
    public UsuarioResponse alternarStatus(Long id) {
        Usuario usuario = carregarComTreinos(id);
        usuario.alternarStatus();
        return UsuarioResponse.comTreinos(usuario);
    }

    @Transactional
    public UsuarioResponse sincronizarTreinos(Long id, List<Long> treinosIds) {
        Usuario usuario = carregarComTreinos(id);
        List<Treino> treinos = treinoRepository.findAllById(treinosIds);

        // findAllById ignora ids inexistentes em silencio; avisamos em vez de
        // gravar um vinculo parcial que o usuario acharia ter salvo por inteiro.
        if (treinos.size() != treinosIds.stream().distinct().count()) {
            throw new RecursoNaoEncontradoException(
                    "Um ou mais treinos informados nao existem mais. Recarregue a lista e tente novamente.");
        }

        usuario.getTreinos().clear();
        usuario.getTreinos().addAll(treinos);

        return UsuarioResponse.comTreinos(usuario);
    }

    private Usuario carregarComTreinos(Long id) {
        return repository.findWithTreinosById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }

    /** Guarda sempre so os digitos, para que a unicidade nao dependa da pontuacao digitada. */
    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
