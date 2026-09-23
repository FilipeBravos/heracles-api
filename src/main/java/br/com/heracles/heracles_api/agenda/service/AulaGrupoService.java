package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.DiaSemana;
import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaNoShowPorHorario;
import br.com.heracles.heracles_api.agenda.dto.LinhaPresencaBruta;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.agenda.repository.InscricaoAulaRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.core.service.NotificacaoService;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.agenda.dto.LinhaFaltaAluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aulas em grupo: agendamento pelo professor/administracao, marcacao de
 * vaga pelo proprio aluno (self-service) ou pela secretaria em nome dele.
 * Turma cheia nao recusa: forma fila de espera, que anda sozinha quando
 * alguem com vaga cancela.
 */
@Service
public class AulaGrupoService {

    /**
     * Amostra minima de ocorrencias pra um horario entrar no relatorio de
     * no-show — um unico dia ruim (ou bom) nao pode decidir a taxa de um
     * horario que so aconteceu uma vez.
     */
    public static final int QUANTIDADE_MINIMA_OCORRENCIAS_PADRAO = 4;

    private final AulaGrupoRepository repository;
    private final InscricaoAulaRepository inscricaoRepository;
    private final AgendamentoPersonalRepository agendamentoPersonalRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final NotificacaoService notificacaoService;

    public AulaGrupoService(AulaGrupoRepository repository,
                            InscricaoAulaRepository inscricaoRepository,
                            AgendamentoPersonalRepository agendamentoPersonalRepository,
                            UsuarioRepository usuarioRepository,
                            UnidadeRepository unidadeRepository,
                            NotificacaoService notificacaoService) {
        this.repository = repository;
        this.inscricaoRepository = inscricaoRepository;
        this.agendamentoPersonalRepository = agendamentoPersonalRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.notificacaoService = notificacaoService;
    }

    /**
     * Agenda operacional: aulas de hoje em diante, ativas, com a ocupacao
     * e a fila de espera de cada uma. Comeca no inicio do dia, nao agora
     * — senao uma aula que aconteceu de manha sumiria da lista a tarde,
     * antes do professor confirmar quem compareceu.
     */
    @Transactional(readOnly = true)
    public Page<AulaGrupoDtos.Response> listar(Pageable pageable) {
        LocalDateTime desde = LocalDate.now().atStartOfDay();
        return repository.findByStatusAndDataHoraGreaterThanEqual(StatusAula.ATIVA, desde, pageable)
                .map(aula -> AulaGrupoDtos.Response.de(aula, vagasOcupadas(aula.getId()), vagasEspera(aula.getId())));
    }

    /** A mesma agenda, sob o olhar do aluno: se ele ja esta inscrito, ou sua posicao na espera. */
    @Transactional(readOnly = true)
    public Page<AulaGrupoDtos.ParaAluno> listarParaAluno(String emailAutenticado, Pageable pageable) {
        Usuario eu = eu(emailAutenticado);
        return repository.findByStatusAndDataHoraGreaterThanEqual(StatusAula.ATIVA, LocalDateTime.now(), pageable)
                .map(aula -> {
                    boolean inscrito = inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(
                            aula.getId(), eu.getId(), StatusInscricao.INSCRITA).isPresent();
                    Integer posicaoEspera = inscrito ? null : posicaoNaEspera(aula.getId(), eu.getId());
                    return AulaGrupoDtos.ParaAluno.de(aula, vagasOcupadas(aula.getId()), inscrito, posicaoEspera);
                });
    }

    @Transactional
    public AulaGrupoDtos.Response criar(AulaGrupoDtos.Salvar request) {
        Usuario professor = usuarioRepository.findById(request.professorId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Professor", request.professorId()));
        if (professor.getTipoPerfil() != TipoPerfil.PROFESSOR) {
            throw new RegraNegocioException(
                    "\"%s\" nao esta cadastrado(a) como professor.".formatted(professor.getNome()));
        }

        Unidade unidade = unidadeRepository.findById(request.unidadeId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", request.unidadeId()));

        LocalDateTime inicio = request.dataHora();
        LocalDateTime fim = inicio.plusMinutes(request.duracaoMinutos());
        garantirSemConflito(professor.getId(), inicio, fim);

        AulaGrupo aula = new AulaGrupo();
        aula.setNome(request.nome());
        aula.setProfessor(professor);
        aula.setUnidade(unidade);
        aula.setDataHora(inicio);
        aula.setDuracaoMinutos(request.duracaoMinutos());
        aula.setCapacidadeMaxima(request.capacidadeMaxima());
        aula.setStatus(StatusAula.ATIVA);

        return AulaGrupoDtos.Response.de(repository.save(aula), 0, 0);
    }

    @Transactional
    public AulaGrupoDtos.Response cancelar(Long aulaId) {
        AulaGrupo aula = buscar(aulaId);
        aula.setStatus(StatusAula.CANCELADA);
        return AulaGrupoDtos.Response.de(aula, vagasOcupadas(aulaId), vagasEspera(aulaId));
    }

    /** Marca a vaga do proprio aluno autenticado — o self-service do app. */
    @Transactional
    public AulaGrupoDtos.ResultadoInscricao inscreverEu(String emailAutenticado, Long aulaId) {
        return inscrever(aulaId, eu(emailAutenticado).getId());
    }

    /**
     * Marca a vaga em nome de outro aluno — o balcao, para quem liga ou
     * passa sem o app. Turma cheia nao recusa: entra na fila de espera.
     */
    @Transactional
    public AulaGrupoDtos.ResultadoInscricao inscrever(Long aulaId, Long alunoId) {
        AulaGrupo aula = buscar(aulaId);
        if (aula.getStatus() != StatusAula.ATIVA) {
            throw new RegraNegocioException("Esta aula foi cancelada.");
        }
        if (aula.getDataHora().isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Esta aula ja aconteceu.");
        }

        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        if (aluno.getTipoPerfil() != TipoPerfil.ALUNO) {
            throw new RegraNegocioException(
                    "\"%s\" nao esta cadastrado(a) como aluno.".formatted(aluno.getNome()));
        }

        if (inscricaoRepository.existsByAulaIdAndAlunoIdAndStatusIn(
                aulaId, alunoId, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA))) {
            throw new RegraNegocioException(
                    "%s ja esta inscrito(a) ou na lista de espera desta aula.".formatted(aluno.getNome()));
        }

        InscricaoAula inscricao = new InscricaoAula();
        inscricao.setAula(aula);
        inscricao.setAluno(aluno);

        if (vagasOcupadas(aulaId) < aula.getCapacidadeMaxima()) {
            inscricao.setStatus(StatusInscricao.INSCRITA);
            inscricaoRepository.save(inscricao);
            return AulaGrupoDtos.ResultadoInscricao.inscrito();
        }

        inscricao.setStatus(StatusInscricao.EM_ESPERA);
        inscricaoRepository.save(inscricao);
        int posicao = (int) vagasEspera(aulaId);
        return AulaGrupoDtos.ResultadoInscricao.emEspera(posicao);
    }

    @Transactional
    public void cancelarInscricaoEu(String emailAutenticado, Long aulaId) {
        cancelarInscricao(aulaId, eu(emailAutenticado).getId());
    }

    /**
     * Cancela a inscricao (marcada ou em espera). Quem estava com vaga
     * marcada libera essa vaga: quem espera ha mais tempo entra na hora,
     * sem precisar de ninguem reabrir a tela.
     */
    @Transactional
    public void cancelarInscricao(Long aulaId, Long alunoId) {
        InscricaoAula inscricao = inscricaoRepository
                .findByAulaIdAndAlunoIdAndStatusIn(
                        aulaId, alunoId, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Inscricao nao encontrada."));

        boolean liberouVaga = inscricao.getStatus() == StatusInscricao.INSCRITA;
        inscricao.cancelar(LocalDateTime.now());

        if (liberouVaga) {
            promoverDaEspera(inscricao.getAula());
        }
    }

    /** Promove quem espera ha mais tempo e avisa — chamado so quando uma vaga marcada acaba de se abrir. */
    private void promoverDaEspera(AulaGrupo aula) {
        inscricaoRepository.findByAulaIdAndStatusOrderByInscritoEmAsc(aula.getId(), StatusInscricao.EM_ESPERA)
                .stream()
                .findFirst()
                .ifPresent(proximo -> {
                    proximo.setStatus(StatusInscricao.INSCRITA);
                    notificacaoService.notificarVagaLiberada(proximo.getAluno(), aula.getId(), aula.getNome());
                });
    }

    // ---------------------------------------------------------------
    // Presenca
    // ---------------------------------------------------------------

    /**
     * O roster da aula para o professor confirmar presenca — so quem tem
     * vaga marcada participa de fato, quem esta na espera ou cancelou nao
     * entra na lista.
     */
    @Transactional(readOnly = true)
    public List<AulaGrupoDtos.LinhaPresenca> listarInscricoesEu(String emailAutenticado, Long aulaId) {
        AulaGrupo aula = buscarComoProfessor(emailAutenticado, aulaId);
        return inscricaoRepository.findByAulaIdAndStatusOrderByAluno_NomeAsc(aula.getId(), StatusInscricao.INSCRITA)
                .stream()
                .map(AulaGrupoDtos.LinhaPresenca::de)
                .toList();
    }

    /**
     * So o professor que deu a aula confirma quem compareceu. "Nao
     * encontrada" tambem cobre tentar confirmar a aula de outro professor,
     * sem revelar que ela existe, so que nao e dele.
     */
    @Transactional
    public void confirmarPresencaEu(String emailAutenticado, Long aulaId, Long alunoId, boolean compareceu) {
        buscarComoProfessor(emailAutenticado, aulaId);

        InscricaoAula inscricao = inscricaoRepository
                .findByAulaIdAndAlunoIdAndStatus(aulaId, alunoId, StatusInscricao.INSCRITA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Inscricao nao encontrada."));

        inscricao.confirmarPresenca(compareceu, LocalDateTime.now());
    }

    /** Taxa de comparecimento geral e o ranking de quem mais falta no periodo. */
    @Transactional(readOnly = true)
    public AulaGrupoDtos.PainelPresenca relatorioPresenca(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();

        long totalConfirmadas = inscricaoRepository.totalConfirmadasDesde(desde);
        long totalFaltas = inscricaoRepository.totalFaltasDesde(desde);
        BigDecimal taxaComparecimento = totalConfirmadas > 0
                ? BigDecimal.valueOf(totalConfirmadas - totalFaltas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalConfirmadas), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<LinhaFaltaAluno> maisFaltosos = inscricaoRepository.faltasPorAlunoDesde(desde, PageRequest.of(0, 10));

        return new AulaGrupoDtos.PainelPresenca(dias, totalConfirmadas, totalFaltas, taxaComparecimento, maisFaltosos);
    }

    /**
     * Taxa de no-show por horario recorrente (mesma aula, unidade, dia da
     * semana e hora) no periodo, do pior pro melhor — nao ha coluna de
     * dia-da-semana na aula (cada ocorrencia e uma linha propria), entao
     * o agrupamento acontece aqui, nao em JPQL. So entra quem tem pelo
     * menos `quantidadeMinima` ocorrencias no periodo.
     */
    @Transactional(readOnly = true)
    public List<LinhaNoShowPorHorario> relatorioNoShowPorHorario(int dias, int quantidadeMinima) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();
        List<LinhaPresencaBruta> brutas = inscricaoRepository.presencasComDetalheDesde(desde);

        Map<ChaveHorario, List<LinhaPresencaBruta>> porHorario = brutas.stream()
                .collect(Collectors.groupingBy(l -> new ChaveHorario(
                        l.nomeAula(), l.unidadeId(), l.dataHora().getDayOfWeek(),
                        l.dataHora().getHour(), l.dataHora().getMinute())));

        return porHorario.entrySet().stream()
                .map(entry -> linhaNoShow(entry.getKey(), entry.getValue()))
                .filter(linha -> linha.ocorrencias() >= quantidadeMinima)
                .sorted((a, b) -> b.taxaNoShow().compareTo(a.taxaNoShow()))
                .toList();
    }

    private LinhaNoShowPorHorario linhaNoShow(ChaveHorario chave, List<LinhaPresencaBruta> linhas) {
        long ocorrencias = linhas.stream().map(LinhaPresencaBruta::dataHora).distinct().count();
        long faltas = linhas.stream().filter(l -> Boolean.FALSE.equals(l.presente())).count();
        long presencas = linhas.stream().filter(l -> Boolean.TRUE.equals(l.presente())).count();
        long total = faltas + presencas;
        BigDecimal taxaNoShow = total > 0
                ? BigDecimal.valueOf(faltas * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LinhaPresencaBruta primeira = linhas.get(0);
        String horario = "%02d:%02d".formatted(chave.hora(), chave.minuto());
        return new LinhaNoShowPorHorario(
                chave.nomeAula(), chave.unidadeId(), primeira.unidadeNome(), primeira.professorNome(),
                converterDiaSemana(chave.diaSemana()), horario, ocorrencias, faltas, presencas, taxaNoShow);
    }

    /** A chave de um horario recorrente: mesma aula, unidade, dia da semana e minuto exato. */
    private record ChaveHorario(String nomeAula, Long unidadeId, DayOfWeek diaSemana, int hora, int minuto) {
    }

    private static DiaSemana converterDiaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    /** Carrega a aula e garante que quem esta autenticado e o professor dela. */
    private AulaGrupo buscarComoProfessor(String emailAutenticado, Long aulaId) {
        AulaGrupo aula = buscar(aulaId);
        Usuario professor = eu(emailAutenticado);
        if (!aula.getProfessor().getId().equals(professor.getId())) {
            throw RecursoNaoEncontradoException.de("Aula", aulaId);
        }
        return aula;
    }

    /** Posicao (1-based) do aluno na fila de espera, ou null se ele nao esta nela. */
    private Integer posicaoNaEspera(Long aulaId, Long alunoId) {
        List<InscricaoAula> fila =
                inscricaoRepository.findByAulaIdAndStatusOrderByInscritoEmAsc(aulaId, StatusInscricao.EM_ESPERA);
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).getAluno().getId().equals(alunoId)) {
                return i + 1;
            }
        }
        return null;
    }

    private long vagasOcupadas(Long aulaId) {
        return inscricaoRepository.countByAulaIdAndStatus(aulaId, StatusInscricao.INSCRITA);
    }

    private long vagasEspera(Long aulaId) {
        return inscricaoRepository.countByAulaIdAndStatus(aulaId, StatusInscricao.EM_ESPERA);
    }

    private AulaGrupo buscar(Long aulaId) {
        return repository.findById(aulaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", aulaId));
    }

    /**
     * O professor nao pode estar em duas agendas ao mesmo tempo — nem duas
     * aulas em grupo, nem uma aula por cima de um personal ja marcado.
     */
    private void garantirSemConflito(Long professorId, LocalDateTime inicio, LocalDateTime fim) {
        boolean cruzaAula = repository.findByProfessorIdAndStatus(professorId, StatusAula.ATIVA).stream()
                .anyMatch(existente -> existente.getDataHora().isBefore(fim) && inicio.isBefore(existente.getFim()));
        if (cruzaAula) {
            throw new RegraNegocioException("O professor ja tem uma aula marcada neste horario.");
        }

        boolean cruzaPersonal = agendamentoPersonalRepository
                .findByProfessorIdAndStatus(professorId, StatusAgendamento.AGENDADO).stream()
                .anyMatch(existente -> existente.getDataHora().isBefore(fim) && inicio.isBefore(existente.getFim()));
        if (cruzaPersonal) {
            throw new RegraNegocioException("O professor ja tem uma sessao de personal marcada neste horario.");
        }
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }
}
