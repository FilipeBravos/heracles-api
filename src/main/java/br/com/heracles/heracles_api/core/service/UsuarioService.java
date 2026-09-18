package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Anamnese;
import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AnamneseDtos;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.repository.AnamneseRepository;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
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

    /** 3 MB decodificados. Foto de perfil, nao arquivo — nao precisa de mais que isso. */
    private static final int TAMANHO_MAXIMO_FOTO_BYTES = 3 * 1024 * 1024;

    private final UsuarioRepository repository;
    private final TreinoRepository treinoRepository;
    private final HistoricoTreinoAlunoRepository historicoTreinoRepository;
    private final AnamneseRepository anamneseRepository;
    private final PlanoRepository planoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          TreinoRepository treinoRepository,
                          HistoricoTreinoAlunoRepository historicoTreinoRepository,
                          AnamneseRepository anamneseRepository,
                          PlanoRepository planoRepository,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.treinoRepository = treinoRepository;
        this.historicoTreinoRepository = historicoTreinoRepository;
        this.anamneseRepository = anamneseRepository;
        this.planoRepository = planoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return repository.buscarPaginadoComTreinos(pageable)
                .map(usuario -> UsuarioResponse.comTreinos(usuario, temAnamnese(usuario.getId())));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = carregarComTreinos(id);
        return UsuarioResponse.comTreinos(usuario, temAnamnese(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequests.Criar request, String emailDeQuemCria) {
        Usuario autor = repository.findByEmailIgnoreCase(emailDeQuemCria)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));

        garantirQuePodeCriar(autor.getTipoPerfil(), request.tipoPerfil());
        garantirDadosDeAlunoCompletos(request.tipoPerfil(), request.endereco(), request.cep(),
                request.dataNascimento(), request.planoEscolhidoId());

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
        usuario.setEndereco(request.endereco());
        usuario.setCep(normalizarCep(request.cep()));
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setTipoPerfil(request.tipoPerfil());
        // A senha e cifrada aqui, no servidor. O cliente nunca envia nem recebe hash.
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));

        aplicarFoto(usuario, request.fotoBase64(), request.fotoContentType());
        aplicarPlanoEscolhido(usuario, request.planoEscolhidoId());

        Usuario salvo = repository.save(usuario);
        return UsuarioResponse.semTreinos(salvo, false);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequests.Atualizar request) {
        Usuario usuario = carregarComTreinos(id);
        garantirDadosDeAlunoCompletos(usuario.getTipoPerfil(), request.endereco(), request.cep(),
                request.dataNascimento(), request.planoEscolhidoId());

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
        usuario.setEndereco(request.endereco());
        usuario.setCep(normalizarCep(request.cep()));
        usuario.setDataNascimento(request.dataNascimento());

        // A foto so muda quando uma nova vem no corpo — diferente do resto
        // do formulario, que e sempre substituicao integral. Reenviar a
        // edicao sem foto nao pode apagar a que ja existe.
        aplicarFoto(usuario, request.fotoBase64(), request.fotoContentType());
        aplicarPlanoEscolhido(usuario, request.planoEscolhidoId());

        return UsuarioResponse.comTreinos(usuario, temAnamnese(id));
    }

    @Transactional
    public UsuarioResponse alternarStatus(Long id) {
        Usuario usuario = carregarComTreinos(id);
        usuario.alternarStatus();
        return UsuarioResponse.comTreinos(usuario, temAnamnese(id));
    }

    /** Bytes da foto e o content-type para a resposta HTTP. Nenhum dos dois vai no UsuarioResponse. */
    @Transactional(readOnly = true)
    public Usuario buscarFoto(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
        if (usuario.getFoto() == null) {
            throw new RecursoNaoEncontradoException("Este usuario nao tem foto cadastrada.");
        }
        return usuario;
    }

    /** A anamnese do aluno, ou o estado "ausente" — nao ter uma ainda e normal, nao erro. */
    @Transactional(readOnly = true)
    public AnamneseDtos.Response buscarAnamnese(Long alunoId) {
        garantirQueExiste(alunoId);
        return anamneseRepository.findByAlunoId(alunoId)
                .map(AnamneseDtos.Response::de)
                .orElseGet(AnamneseDtos.Response::ausente);
    }

    /** Cria a anamnese na primeira vez, ou substitui a existente — sempre por inteiro. */
    @Transactional
    public AnamneseDtos.Response salvarAnamnese(Long alunoId, AnamneseDtos.Salvar request) {
        Usuario aluno = repository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));

        Anamnese anamnese = anamneseRepository.findByAlunoId(alunoId).orElseGet(Anamnese::new);
        anamnese.setAluno(aluno);
        anamnese.setObjetivo(request.objetivo());
        anamnese.setCondicoesSaude(vazioComoNulo(request.condicoesSaude()));
        anamnese.setLesoesCirurgias(vazioComoNulo(request.lesoesCirurgias()));
        anamnese.setMedicamentosUso(vazioComoNulo(request.medicamentosUso()));
        anamnese.setRestricoesMedicas(vazioComoNulo(request.restricoesMedicas()));
        anamnese.setContatoEmergenciaNome(request.contatoEmergenciaNome());
        anamnese.setContatoEmergenciaTelefone(request.contatoEmergenciaTelefone());
        anamnese.setPreenchidaEm(LocalDateTime.now());

        return AnamneseDtos.Response.de(anamneseRepository.save(anamnese));
    }

    /**
     * Sincroniza a lista de fichas do aluno e registra a troca no historico.
     *
     * O vinculo em si (Usuario.treinos) sempre foi um retrato do agora: a
     * proxima chamada substitui a lista inteira, sem dizer qual ficha saiu
     * nem por quanto tempo o aluno ficou nela. O historico e o registro
     * paralelo que guarda isso — fecha o periodo de quem sai, abre o de
     * quem entra, e nao toca em quem continua.
     *
     * Receber uma ficha nova exige anamnese preenchida — desvincular tudo
     * (lista vazia) continua liberado sempre, porque tirar uma ficha nunca
     * expos ninguem a um risco que a anamnese preveniria.
     */
    @Transactional
    public UsuarioResponse sincronizarTreinos(Long id, List<Long> treinosIds) {
        Usuario usuario = carregarComTreinos(id);

        if (!treinosIds.isEmpty() && !anamneseRepository.existsByAlunoId(id)) {
            throw new RegraNegocioException(
                    "%s precisa preencher a anamnese antes de receber uma ficha."
                            .formatted(usuario.getNome()));
        }

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

        return UsuarioResponse.comTreinos(usuario, temAnamnese(id));
    }

    private Usuario carregarComTreinos(Long id) {
        return repository.findWithTreinosById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }

    private void garantirQueExiste(Long id) {
        if (!repository.existsById(id)) {
            throw RecursoNaoEncontradoException.de("Aluno", id);
        }
    }

    private boolean temAnamnese(Long usuarioId) {
        return anamneseRepository.existsByAlunoId(usuarioId);
    }

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

    /**
     * Endereco, CEP, data de nascimento e plano escolhido so importam para
     * o aluno — professor, secretaria e admin sao conta de acesso, nao tem
     * "plano escolhido" nem precisam de endereco cadastrado. Por isso a
     * obrigatoriedade e regra aqui, condicionada ao perfil, e nao anotacao
     * estatica no DTO: o mesmo contrato serve os quatro perfis.
     */
    private void garantirDadosDeAlunoCompletos(TipoPerfil perfil, String endereco, String cep,
                                                LocalDate dataNascimento, Long planoEscolhidoId) {
        if (perfil != TipoPerfil.ALUNO) {
            return;
        }
        if (isBlank(endereco)) {
            throw new RegraNegocioException("O endereco e obrigatorio para o cadastro de aluno.");
        }
        if (isBlank(cep)) {
            throw new RegraNegocioException("O CEP e obrigatorio para o cadastro de aluno.");
        }
        if (dataNascimento == null) {
            throw new RegraNegocioException("A data de nascimento e obrigatoria para o cadastro de aluno.");
        }
        if (planoEscolhidoId == null) {
            throw new RegraNegocioException("Escolha o plano do aluno para concluir o cadastro.");
        }
    }

    /**
     * So a escolha do aluno, sem virar assinatura — a matricula de fato
     * nasce em matriculas.assinaturas quando a secretaria confirma pela
     * tela de Matriculas. `null` limpa a escolha: Atualizar substitui por
     * inteiro, como o resto do cadastro.
     */
    private void aplicarPlanoEscolhido(Usuario usuario, Long planoEscolhidoId) {
        if (planoEscolhidoId == null) {
            usuario.setPlanoEscolhido(null);
            return;
        }
        Plano plano = planoRepository.findById(planoEscolhidoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", planoEscolhidoId));
        if (!plano.isAtivo()) {
            throw new RegraNegocioException(
                    "O plano \"%s\" esta fora de linha e nao pode ser escolhido.".formatted(plano.getNome()));
        }
        usuario.setPlanoEscolhido(plano);
    }

    /**
     * Sem foto enviada, nao mexe no que ja existe — e o unico campo do
     * cadastro com esse comportamento (ver o comentario em atualizar()).
     */
    private void aplicarFoto(Usuario usuario, String fotoBase64, String fotoContentType) {
        if (fotoBase64 == null && fotoContentType == null) {
            return;
        }
        if (fotoBase64 == null || fotoContentType == null) {
            throw new RegraNegocioException("Envie a foto e o tipo de conteudo juntos.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(fotoBase64);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Foto em formato invalido.");
        }
        if (bytes.length > TAMANHO_MAXIMO_FOTO_BYTES) {
            throw new RegraNegocioException("A foto excede o tamanho maximo de 3 MB.");
        }

        usuario.setFoto(bytes);
        usuario.setFotoContentType(fotoContentType);
    }

    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /** Mesma ideia do CPF: guarda so os digitos, para "01310-100" e "01310100" serem o mesmo CEP. */
    private String normalizarCep(String cep) {
        return cep == null ? null : cep.replaceAll("\\D", "");
    }

    private String vazioComoNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
