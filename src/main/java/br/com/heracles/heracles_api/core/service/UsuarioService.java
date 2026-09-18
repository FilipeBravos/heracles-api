package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final HistoricoTreinoAlunoRepository historicoTreinoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          TreinoRepository treinoRepository,
                          HistoricoTreinoAlunoRepository historicoTreinoRepository,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.treinoRepository = treinoRepository;
        this.historicoTreinoRepository = historicoTreinoRepository;
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
    public UsuarioResponse criar(UsuarioRequests.Criar request, String emailDeQuemCria) {
        Usuario autor = repository.findByEmailIgnoreCase(emailDeQuemCria)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));

        garantirQuePodeCriar(autor.getTipoPerfil(), request.tipoPerfil());

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

    /**
     * Sincroniza a lista de fichas do aluno e registra a troca no historico.
     *
     * O vinculo em si (Usuario.treinos) sempre foi um retrato do agora: a
     * proxima chamada substitui a lista inteira, sem dizer qual ficha saiu
     * nem por quanto tempo o aluno ficou nela. O historico e o registro
     * paralelo que guarda isso — fecha o periodo de quem sai, abre o de
     * quem entra, e nao toca em quem continua.
     */
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

        LocalDateTime agora = LocalDateTime.now();
        Set<Long> antes = usuario.getTreinos().stream().map(Treino::getId).collect(Collectors.toSet());
        Set<Long> depois = new HashSet<>(treinosIds);

        for (Treino treino : usuario.getTreinos()) {
            if (!depois.contains(treino.getId())) {
                historicoTreinoRepository.buscarAbertoPorAlunoETreino(usuario.getId(), treino.getId())
                        .ifPresent(historico -> historico.encerrar(agora));
            }
        }
        for (Treino treino : treinos) {
            if (!antes.contains(treino.getId())) {
                HistoricoTreinoAluno historico = new HistoricoTreinoAluno();
                historico.setAluno(usuario);
                historico.setTreino(treino);
                historico.setTreinoNome(treino.getNome());
                historico.setTreinoFoco(treino.getFoco());
                historico.setTreinoNivel(treino.getNivel());
                historico.setVinculadoEm(agora);
                historicoTreinoRepository.save(historico);
            }
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
    /**
     * Quem cria quem.
     *
     * Matricular e da recepcao: o cadastro do aluno acompanha a matricula,
     * e quem recebe o aluno no balcao e quem tem os documentos na mao.
     *
     * O resto — professor, secretaria, outro admin — e da administracao. E
     * isto que fecha a escalacao: `tipoPerfil` vem do corpo da requisicao,
     * entao sem esta regra bastava a secretaria mandar "ADMIN" para criar
     * uma conta de administrador, entrar com ela e fazer o que quisesse.
     */
    private void garantirQuePodeCriar(TipoPerfil autor, TipoPerfil perfilDesejado) {
        boolean permitido = perfilDesejado == TipoPerfil.ALUNO
                ? autor == TipoPerfil.SECRETARIA
                : autor == TipoPerfil.ADMIN;

        if (!permitido) {
            throw new RegraNegocioException(perfilDesejado == TipoPerfil.ALUNO
                    ? "Cadastro de aluno e da secretaria."
                    : "Cadastro de %s e da administracao.".formatted(
                            perfilDesejado.name().toLowerCase()));
        }
    }

    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
