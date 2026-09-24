package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Anamnese;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import br.com.heracles.heracles_api.core.domain.ContratoAssinado;
import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AnamneseDtos;
import br.com.heracles.heracles_api.core.dto.Aniversariante;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.dto.ContratoDtos;
import br.com.heracles.heracles_api.core.dto.LinhaAlunoAnamnese;
import br.com.heracles.heracles_api.core.dto.LinhaAvaliacaoParaEvolucao;
import br.com.heracles.heracles_api.core.dto.LinhaCoberturaAnamnesePorUnidade;
import br.com.heracles.heracles_api.core.dto.LinhaEvolucaoFisicaPorUnidade;
import br.com.heracles.heracles_api.core.dto.LinhaReavaliacaoVencida;
import br.com.heracles.heracles_api.core.dto.ResumoReavaliacaoVencida;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.repository.AnamneseRepository;
import br.com.heracles.heracles_api.core.repository.AvaliacaoFisicaRepository;
import br.com.heracles.heracles_api.core.repository.ContratoAssinadoRepository;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.dto.AlunoUnidade;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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

    /**
     * Janela padrao do alerta de reavaliacao fisica vencida — 3 meses,
     * cadencia comum de reavaliacao em academia. Curta o bastante pra
     * pegar quem parou de vir reavaliar, longa o bastante pra nao soar
     * como cobranca de quem acabou de fazer uma.
     */
    public static final int DIAS_REAVALIACAO_PADRAO = 90;

    /**
     * Janela padrao da evolucao fisica media — mais larga que a de
     * reavaliacao vencida porque aqui o interesse e a tendencia ao longo
     * de meses, nao detectar quem parou de reavaliar recentemente.
     */
    public static final int DIAS_EVOLUCAO_FISICA_PADRAO = 365;

    /**
     * Texto vigente do contrato de adesao. Fixo por enquanto — nao ha tela
     * de edicao de modelo de contrato nesta entrega, so a assinatura dele.
     * Uma edicao aqui nao altera contratos ja assinados: cada um grava sua
     * propria copia em ContratoAssinado.textoContrato no momento da assinatura.
     */
    public static final String TEXTO_CONTRATO_PADRAO = """
            CONTRATO DE ADESAO - HERACLES ACADEMIA

            Ao assinar este contrato, o(a) aluno(a) concorda com as \
            condicoes gerais de uso das instalacoes e servicos da unidade, \
            incluindo o pagamento pontual da mensalidade do plano escolhido, \
            o uso adequado dos equipamentos e o respeito as normas internas \
            de convivencia e seguranca. A academia se compromete a manter \
            as instalacoes em condicoes adequadas de uso e a prestar os \
            servicos contratados com qualidade.""";

    private final UsuarioRepository repository;
    private final TreinoRepository treinoRepository;
    private final HistoricoTreinoAlunoRepository historicoTreinoRepository;
    private final AnamneseRepository anamneseRepository;
    private final AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    private final ContratoAssinadoRepository contratoAssinadoRepository;
    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          TreinoRepository treinoRepository,
                          HistoricoTreinoAlunoRepository historicoTreinoRepository,
                          AnamneseRepository anamneseRepository,
                          AvaliacaoFisicaRepository avaliacaoFisicaRepository,
                          ContratoAssinadoRepository contratoAssinadoRepository,
                          PlanoRepository planoRepository,
                          AssinaturaRepository assinaturaRepository,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.treinoRepository = treinoRepository;
        this.historicoTreinoRepository = historicoTreinoRepository;
        this.anamneseRepository = anamneseRepository;
        this.avaliacaoFisicaRepository = avaliacaoFisicaRepository;
        this.contratoAssinadoRepository = contratoAssinadoRepository;
        this.planoRepository = planoRepository;
        this.assinaturaRepository = assinaturaRepository;
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
    public UsuarioResponse criar(UsuarioRequests.Criar request, String emailDeQuemCria, String ipOrigem) {
        Usuario autor = repository.findByEmailIgnoreCase(emailDeQuemCria)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));

        garantirQuePodeCriar(autor.getTipoPerfil(), request.tipoPerfil());
        garantirDadosDeAlunoCompletos(request.tipoPerfil(), request.endereco(), request.cep(),
                request.dataNascimento(), request.planoEscolhidoId());
        garantirContratoAssinado(request.tipoPerfil(), request.nomeAssinaturaContrato(), request.aceiteContrato());

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

        if (salvo.getTipoPerfil() == TipoPerfil.ALUNO) {
            assinarContrato(salvo, request.nomeAssinaturaContrato(), ipOrigem);
        }

        return UsuarioResponse.semTreinos(salvo, false);
    }

    private void assinarContrato(Usuario aluno, String nomeDigitado, String ipOrigem) {
        ContratoAssinado contrato = new ContratoAssinado();
        contrato.setAluno(aluno);
        contrato.setNomeDigitado(nomeDigitado.trim());
        contrato.setTextoContrato(TEXTO_CONTRATO_PADRAO);
        contrato.setIpOrigem(ipOrigem);
        contratoAssinadoRepository.save(contrato);
    }

    /** O contrato assinado no cadastro, ou o estado "ausente" — cadastros anteriores a esta funcionalidade nao tem um. */
    @Transactional(readOnly = true)
    public ContratoDtos.Response buscarContrato(Long alunoId) {
        garantirQueExiste(alunoId);
        return contratoAssinadoRepository.findByAlunoId(alunoId)
                .map(ContratoDtos.Response::de)
                .orElseGet(ContratoDtos.Response::ausente);
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

    /**
     * O historico de avaliacoes fisicas do aluno, mais recente primeiro.
     *
     * Diferente da anamnese, nao ha "ausente vira um objeto vazio": uma
     * lista vazia ja diz tudo, e nenhuma tela precisa de outro sinal.
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoFisicaDtos.Response> historicoAvaliacoesFisicas(Long alunoId) {
        garantirQueExiste(alunoId);
        return avaliacaoFisicaRepository.findByAlunoIdOrderByDataDesc(alunoId).stream()
                .map(AvaliacaoFisicaDtos.Response::de)
                .toList();
    }

    /**
     * Registra uma avaliacao fisica nova — nunca edita uma existente.
     *
     * A anamnese e uma so, atualizada no lugar; a avaliacao fisica e
     * periodica, e a evolucao esta em comparar uma com a anterior. Uma
     * medida errada se corrige com uma avaliacao nova, nao reescrevendo
     * o passado.
     */
    @Transactional
    public AvaliacaoFisicaDtos.Response registrarAvaliacaoFisica(Long alunoId, AvaliacaoFisicaDtos.Salvar request) {
        Usuario aluno = repository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));

        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setAluno(aluno);
        avaliacao.setData(request.data() != null ? request.data() : LocalDate.now());
        avaliacao.setPesoKg(request.pesoKg());
        avaliacao.setAlturaCm(request.alturaCm());
        avaliacao.setPercentualGordura(request.percentualGordura());
        avaliacao.setCircunferenciaCintura(request.circunferenciaCintura());
        avaliacao.setCircunferenciaQuadril(request.circunferenciaQuadril());
        avaliacao.setCircunferenciaBraco(request.circunferenciaBraco());
        avaliacao.setCircunferenciaCoxa(request.circunferenciaCoxa());
        avaliacao.setCircunferenciaPeito(request.circunferenciaPeito());
        avaliacao.setObservacoes(vazioComoNulo(request.observacoes()));
        if (request.fotos() != null) {
            for (AvaliacaoFisicaDtos.Foto fotoRequest : request.fotos()) {
                AvaliacaoFisicaFoto foto = new AvaliacaoFisicaFoto();
                foto.setFoto(decodificarFoto(fotoRequest.base64()));
                foto.setFotoContentType(fotoRequest.contentType());
                avaliacao.adicionarFoto(foto);
            }
        }

        return AvaliacaoFisicaDtos.Response.de(avaliacaoFisicaRepository.save(avaliacao));
    }

    /**
     * Uma foto da galeria de evolucao. Confere que a avaliacao e mesmo do
     * aluno da rota — sem isso, o id da avaliacao bastaria pra ver a foto
     * de qualquer aluno.
     */
    @Transactional(readOnly = true)
    public AvaliacaoFisicaFoto buscarFotoAvaliacaoFisica(Long alunoId, Long avaliacaoId, Long fotoId) {
        AvaliacaoFisica avaliacao = buscarAvaliacaoDoAluno(alunoId, avaliacaoId);
        return avaliacao.getFotos().stream()
                .filter(foto -> foto.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Foto", fotoId));
    }

    /**
     * O comparativo entre duas avaliacoes do aluno — a primeira e a mais
     * recente por padrao, ou duas escolhidas via deId/paraId.
     *
     * Menos de duas avaliacoes nao e erro: e o estado normal de quem
     * acabou de comecar a acompanhar a evolucao, e a tela precisa saber
     * disso para mostrar "ainda sem comparativo" em vez de uma falha.
     */
    @Transactional(readOnly = true)
    public AvaliacaoFisicaDtos.Comparativo compararAvaliacoesFisicas(Long alunoId, Long deId, Long paraId) {
        garantirQueExiste(alunoId);

        AvaliacaoFisica de;
        AvaliacaoFisica para;
        if (deId == null && paraId == null) {
            Optional<AvaliacaoFisica> primeira = avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataAsc(alunoId);
            Optional<AvaliacaoFisica> maisRecente = avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataDesc(alunoId);
            if (primeira.isEmpty() || maisRecente.isEmpty()
                    || primeira.get().getId().equals(maisRecente.get().getId())) {
                return AvaliacaoFisicaDtos.Comparativo.indisponivel();
            }
            de = primeira.get();
            para = maisRecente.get();
        } else if (deId != null && paraId != null) {
            de = buscarAvaliacaoDoAluno(alunoId, deId);
            para = buscarAvaliacaoDoAluno(alunoId, paraId);
        } else {
            throw new RegraNegocioException(
                    "Informe as duas avaliacoes a comparar, ou nenhuma para usar a primeira e a mais recente.");
        }

        return AvaliacaoFisicaDtos.Comparativo.de(de, para);
    }

    /** Aniversariantes do mes corrente, do dia mais proximo pro mais distante. */
    @Transactional(readOnly = true)
    public List<Aniversariante> aniversariantesDoMes() {
        int mes = LocalDate.now().getMonthValue();
        return repository.buscarAniversariantesDoMes(mes).stream().map(Aniversariante::de).toList();
    }

    /** Cabecalho do alerta: quantos alunos com matricula ativa estao com a reavaliacao fisica vencida. */
    @Transactional(readOnly = true)
    public ResumoReavaliacaoVencida resumoReavaliacaoVencida(int diasSemReavaliacao) {
        LocalDate limite = LocalDate.now().minusDays(diasSemReavaliacao);
        return new ResumoReavaliacaoVencida(repository.countReavaliacaoVencida(limite));
    }

    /**
     * O alerta em si: matricula ativa, mas a ultima avaliacao fisica
     * passou da janela — indicador antecedente de qualidade de
     * atendimento, do mesmo jeito que o alerta de ficha ausente, so que
     * sobre acompanhamento continuo em vez de recebimento inicial.
     */
    @Transactional(readOnly = true)
    public Page<LinhaReavaliacaoVencida> reavaliacaoVencida(Pageable pageable, int diasSemReavaliacao) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.minusDays(diasSemReavaliacao);
        return repository.buscarReavaliacaoVencida(limite, pageable)
                .map(bruta -> LinhaReavaliacaoVencida.de(bruta, hoje));
    }

    /**
     * Evolucao fisica media por unidade: media do delta de peso,
     * percentual de gordura e IMC entre a primeira e a ultima avaliacao de
     * cada aluno no periodo, agregada por unidade. So entra aluno com pelo
     * menos duas avaliacoes no periodo — com uma so nao ha o que comparar.
     *
     * A unidade vem da assinatura vigente do aluno, nao de quando a
     * avaliacao foi feita: AvaliacaoFisica nao carrega unidade, e
     * Assinatura e um retrato do agora, sem historico de intervalos — e a
     * melhor aproximacao disponivel. Aluno sem assinatura vigente fica de
     * fora do relatorio por unidade, mesmo tendo evolucao calculada.
     */
    @Transactional(readOnly = true)
    public List<LinhaEvolucaoFisicaPorUnidade> evolucaoFisicaMediaPorUnidade(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias);
        List<LinhaAvaliacaoParaEvolucao> avaliacoes = avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(desde);

        Map<Long, DeltaAluno> deltasPorAluno = avaliacoes.stream()
                .collect(Collectors.groupingBy(LinhaAvaliacaoParaEvolucao::alunoId))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> calcularDelta(entry.getValue())));

        if (deltasPorAluno.isEmpty()) {
            return List.of();
        }

        Map<Long, List<AlunoUnidade>> unidadesPorAluno = assinaturaRepository
                .buscarUnidadesVigentesPorAlunos(deltasPorAluno.keySet()).stream()
                .collect(Collectors.groupingBy(AlunoUnidade::alunoId));

        Map<Long, List<ContribuicaoUnidade>> porUnidade = deltasPorAluno.entrySet().stream()
                .flatMap(entry -> unidadesPorAluno.getOrDefault(entry.getKey(), List.of()).stream()
                        .map(au -> new ContribuicaoUnidade(au.unidadeId(), au.unidadeNome(), entry.getValue())))
                .collect(Collectors.groupingBy(ContribuicaoUnidade::unidadeId));

        return porUnidade.values().stream()
                .map(this::linhaEvolucao)
                .sorted(Comparator.comparing(LinhaEvolucaoFisicaPorUnidade::unidadeNome))
                .toList();
    }

    private LinhaEvolucaoFisicaPorUnidade linhaEvolucao(List<ContribuicaoUnidade> contribuicoes) {
        return new LinhaEvolucaoFisicaPorUnidade(
                contribuicoes.get(0).unidadeNome(),
                contribuicoes.size(),
                mediaDelta(contribuicoes, DeltaAluno::deltaPeso),
                mediaDelta(contribuicoes, DeltaAluno::deltaPercentualGordura),
                mediaDelta(contribuicoes, DeltaAluno::deltaImc));
    }

    private BigDecimal mediaDelta(List<ContribuicaoUnidade> contribuicoes, Function<DeltaAluno, BigDecimal> campo) {
        List<BigDecimal> valores = contribuicoes.stream()
                .map(ContribuicaoUnidade::delta)
                .map(campo)
                .filter(Objects::nonNull)
                .toList();
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP);
    }

    /** As avaliacoes do aluno ja vem ordenadas por data (ver avaliacoesParaEvolucaoDesde) — a primeira e a ultima da lista bastam. */
    private DeltaAluno calcularDelta(List<LinhaAvaliacaoParaEvolucao> avaliacoesDoAluno) {
        LinhaAvaliacaoParaEvolucao primeira = avaliacoesDoAluno.get(0);
        LinhaAvaliacaoParaEvolucao ultima = avaliacoesDoAluno.get(avaliacoesDoAluno.size() - 1);
        return new DeltaAluno(
                subtrairSeAmbosPresentes(ultima.pesoKg(), primeira.pesoKg()),
                subtrairSeAmbosPresentes(ultima.percentualGordura(), primeira.percentualGordura()),
                subtrairSeAmbosPresentes(calcularImc(ultima), calcularImc(primeira)));
    }

    private static BigDecimal subtrairSeAmbosPresentes(BigDecimal maisRecente, BigDecimal maisAntiga) {
        return (maisRecente == null || maisAntiga == null) ? null : maisRecente.subtract(maisAntiga);
    }

    private static BigDecimal calcularImc(LinhaAvaliacaoParaEvolucao avaliacao) {
        if (avaliacao.pesoKg() == null || avaliacao.alturaCm() == null || avaliacao.alturaCm().signum() == 0) {
            return null;
        }
        BigDecimal alturaM = avaliacao.alturaCm().divide(BigDecimal.valueOf(100));
        return avaliacao.pesoKg().divide(alturaM.multiply(alturaM), 1, RoundingMode.HALF_UP);
    }

    /** O delta de um aluno entre a primeira e a ultima avaliacao do periodo — um campo nulo significa "sem dado", nao zero. */
    private record DeltaAluno(BigDecimal deltaPeso, BigDecimal deltaPercentualGordura, BigDecimal deltaImc) {
    }

    /** O delta de um aluno atribuido a uma das unidades vigentes dele. */
    private record ContribuicaoUnidade(Long unidadeId, String unidadeNome, DeltaAluno delta) {
    }

    /**
     * Cobertura de anamnese por unidade: entre alunos com matricula
     * vigente, quantos ja preencheram a anamnese, em percentual — do pior
     * pro melhor.
     *
     * A unidade vem da assinatura vigente de cada aluno, mesmo
     * espalhamento de evolucaoFisicaMediaPorUnidade: um aluno de plano de
     * rede conta uma vez em cada unidade que o plano cobre.
     */
    @Transactional(readOnly = true)
    public List<LinhaCoberturaAnamnesePorUnidade> coberturaAnamnesePorUnidade() {
        List<LinhaAlunoAnamnese> alunos = repository.buscarAlunosVigentesComAnamnese();
        if (alunos.isEmpty()) {
            return List.of();
        }

        List<Long> alunoIds = alunos.stream().map(LinhaAlunoAnamnese::alunoId).toList();
        Map<Long, List<AlunoUnidade>> unidadesPorAluno = assinaturaRepository
                .buscarUnidadesVigentesPorAlunos(alunoIds).stream()
                .collect(Collectors.groupingBy(AlunoUnidade::alunoId));

        Map<Long, List<ContribuicaoAnamnese>> porUnidade = alunos.stream()
                .flatMap(l -> unidadesPorAluno.getOrDefault(l.alunoId(), List.of()).stream()
                        .map(au -> new ContribuicaoAnamnese(au.unidadeId(), au.unidadeNome(), l.temAnamnese())))
                .collect(Collectors.groupingBy(ContribuicaoAnamnese::unidadeId));

        return porUnidade.values().stream()
                .map(this::linhaCoberturaAnamnese)
                .sorted(Comparator.comparing(LinhaCoberturaAnamnesePorUnidade::percentualCobertura))
                .toList();
    }

    private LinhaCoberturaAnamnesePorUnidade linhaCoberturaAnamnese(List<ContribuicaoAnamnese> contribuicoes) {
        long total = contribuicoes.size();
        long comAnamnese = contribuicoes.stream().filter(ContribuicaoAnamnese::temAnamnese).count();
        BigDecimal percentual = BigDecimal.valueOf(comAnamnese * 100.0 / total).setScale(1, RoundingMode.HALF_UP);

        return new LinhaCoberturaAnamnesePorUnidade(
                contribuicoes.get(0).unidadeNome(), total, comAnamnese, percentual);
    }

    /** Se um aluno com matricula vigente, atribuido a uma das unidades vigentes dele, tem anamnese preenchida. */
    private record ContribuicaoAnamnese(Long unidadeId, String unidadeNome, boolean temAnamnese) {
    }

    private AvaliacaoFisica buscarAvaliacaoDoAluno(Long alunoId, Long avaliacaoId) {
        AvaliacaoFisica avaliacao = avaliacaoFisicaRepository.findById(avaliacaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Avaliacao fisica", avaliacaoId));
        if (!avaliacao.getAluno().getId().equals(alunoId)) {
            throw RecursoNaoEncontradoException.de("Avaliacao fisica", avaliacaoId);
        }
        return avaliacao;
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
     * estatica no DTO: o mesmo contrato de entrada serve os quatro perfis.
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
     * A assinatura do contrato so se exige na criacao — atualizar dados
     * cadastrais nao reabre o contrato ja assinado. Substitui "so senha
     * inicial" como o momento de aceite: sem nome digitado e aceite
     * marcado, a API recusa o cadastro do aluno.
     */
    private void garantirContratoAssinado(TipoPerfil perfil, String nomeAssinaturaContrato, Boolean aceiteContrato) {
        if (perfil != TipoPerfil.ALUNO) {
            return;
        }
        if (isBlank(nomeAssinaturaContrato)) {
            throw new RegraNegocioException("Assine o contrato com o nome completo para concluir o cadastro.");
        }
        if (aceiteContrato == null || !aceiteContrato) {
            throw new RegraNegocioException("E preciso aceitar os termos do contrato para concluir o cadastro.");
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
        usuario.setFoto(decodificarFoto(fotoBase64));
        usuario.setFotoContentType(fotoContentType);
    }

    private byte[] decodificarFoto(String fotoBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(fotoBase64);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Foto em formato invalido.");
        }
        if (bytes.length > TAMANHO_MAXIMO_FOTO_BYTES) {
            throw new RegraNegocioException("A foto excede o tamanho maximo de 3 MB.");
        }
        return bytes;
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
