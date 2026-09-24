package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.DiaSemana;
import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaCancelamentoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaCoberturaHorario;
import br.com.heracles.heracles_api.agenda.dto.LinhaMinutosOcupadosProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaOcupacaoPersonal;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessaoPersonalFinalizada;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessoesPorProfessor;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.agenda.repository.HorarioProfessorRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sessoes de personal: agendadas pela secretaria/administracao a pedido do
 * aluno, nunca self-service — diferente da aula em grupo, personal
 * continua um atendimento marcado por gente.
 */
@Service
public class AgendamentoPersonalService {

    /**
     * Amostra minima pra um professor entrar no ranking de avaliacoes —
     * baixa o bastante pra nao excluir quem comecou ha pouco tempo, alta
     * o bastante pra uma unica sessao nao decidir a posicao sozinha.
     */
    public static final long QUANTIDADE_MINIMA_AVALIACOES_PADRAO = 3;

    /** Menos que isso de antecedencia, o cancelamento conta como "em cima da hora". */
    public static final long LIMITE_HORAS_CANCELAMENTO_EM_CIMA_DA_HORA = 24;

    /** Mesmo raciocinio de QUANTIDADE_MINIMA_AVALIACOES_PADRAO, agora para a taxa de cancelamento. */
    public static final long QUANTIDADE_MINIMA_SESSOES_CANCELAMENTO_PADRAO = 4;

    /**
     * Faixa fixa de horario comercial para a cobertura de horario — nao ha
     * abertura/fechamento configuravel por unidade no sistema ainda, e
     * 06h-22h cobre a operacao tipica de uma academia.
     */
    private static final LocalTime INICIO_HORARIO_COMERCIAL = LocalTime.of(6, 0);
    private static final LocalTime FIM_HORARIO_COMERCIAL = LocalTime.of(22, 0);
    private static final int DURACAO_BLOCO_COBERTURA_MINUTOS = 30;

    private final AgendamentoPersonalRepository repository;
    private final AulaGrupoRepository aulaGrupoRepository;
    private final HorarioProfessorRepository horarioProfessorRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;

    public AgendamentoPersonalService(AgendamentoPersonalRepository repository,
                                      AulaGrupoRepository aulaGrupoRepository,
                                      HorarioProfessorRepository horarioProfessorRepository,
                                      UsuarioRepository usuarioRepository,
                                      UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.aulaGrupoRepository = aulaGrupoRepository;
        this.horarioProfessorRepository = horarioProfessorRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoPersonalDtos.Response> listar(Long professorId, Long alunoId, Pageable pageable) {
        Page<AgendamentoPersonal> pagina;
        if (professorId != null) {
            pagina = repository.findByProfessorId(professorId, pageable);
        } else if (alunoId != null) {
            pagina = repository.findByAlunoId(alunoId, pageable);
        } else {
            pagina = repository.findAllBy(pageable);
        }
        return pagina.map(AgendamentoPersonalDtos.Response::de);
    }

    /** Sessoes do proprio aluno autenticado — so leitura, quem marca continua sendo o balcao. */
    @Transactional(readOnly = true)
    public Page<AgendamentoPersonalDtos.Response> listarParaAluno(String emailAutenticado, Pageable pageable) {
        return listar(null, eu(emailAutenticado).getId(), pageable);
    }

    @Transactional
    public AgendamentoPersonalDtos.Response criar(AgendamentoPersonalDtos.Salvar request) {
        Usuario aluno = usuarioRepository.findById(request.alunoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", request.alunoId()));
        if (aluno.getTipoPerfil() != TipoPerfil.ALUNO) {
            throw new RegraNegocioException(
                    "\"%s\" nao esta cadastrado(a) como aluno.".formatted(aluno.getNome()));
        }

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

        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(inicio);
        sessao.setDuracaoMinutos(request.duracaoMinutos());
        sessao.setObservacoes(vazioComoNulo(request.observacoes()));
        sessao.setStatus(StatusAgendamento.AGENDADO);

        return AgendamentoPersonalDtos.Response.de(repository.save(sessao));
    }

    @Transactional
    public AgendamentoPersonalDtos.Response cancelar(Long id) {
        AgendamentoPersonal sessao = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Sessao de personal", id));
        sessao.cancelar(LocalDateTime.now());
        return AgendamentoPersonalDtos.Response.de(sessao);
    }

    /**
     * O professor confirma que a propria sessao aconteceu — so ele estava
     * la para atestar. "Nao encontrada" tambem cobre tentar confirmar a
     * sessao de outro professor: sem revelar que ela existe, so que nao e
     * dele.
     */
    @Transactional
    public AgendamentoPersonalDtos.Response marcarRealizadaEu(String emailAutenticado, Long id) {
        AgendamentoPersonal sessao = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Sessao de personal", id));
        Usuario professor = eu(emailAutenticado);
        if (!sessao.getProfessor().getId().equals(professor.getId())) {
            throw RecursoNaoEncontradoException.de("Sessao de personal", id);
        }
        sessao.marcarRealizada(LocalDateTime.now());
        return AgendamentoPersonalDtos.Response.de(sessao);
    }

    /**
     * O aluno avalia a propria sessao ja realizada. Mesma logica de
     * "nao encontrada" para sessao de outro aluno.
     */
    @Transactional
    public AgendamentoPersonalDtos.Response avaliarEu(String emailAutenticado, Long id,
                                                       AgendamentoPersonalDtos.Avaliar request) {
        AgendamentoPersonal sessao = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Sessao de personal", id));
        Usuario aluno = eu(emailAutenticado);
        if (!sessao.getAluno().getId().equals(aluno.getId())) {
            throw RecursoNaoEncontradoException.de("Sessao de personal", id);
        }
        sessao.avaliar(request.nota(), vazioComoNulo(request.comentario()));
        return AgendamentoPersonalDtos.Response.de(sessao);
    }

    /**
     * Nota media por professor, do melhor pro pior — visibilidade de
     * qualidade de atendimento pra gestao. `quantidadeMinima` filtra quem
     * ainda nao tem amostra suficiente pra sustentar uma media confiavel.
     */
    @Transactional(readOnly = true)
    public List<LinhaAvaliacaoProfessor> mediaAvaliacaoPorProfessor(long quantidadeMinima) {
        return repository.mediaAvaliacaoPorProfessor(quantidadeMinima);
    }

    /**
     * Ranking de sessoes de personal realizadas por professor, do mais
     * cheio pro menos cheio — visibilidade direta de volume de
     * atendimento, sem cruzar com taxa de cancelamento nem ocupacao da
     * agenda, que ja tem relatorio proprio.
     */
    @Transactional(readOnly = true)
    public List<LinhaSessoesPorProfessor> sessoesRealizadasPorProfessor(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();
        return repository.contarSessoesRealizadasPorProfessorDesde(desde);
    }

    /**
     * Taxa de cancelamento em cima da hora por professor, do pior pro
     * melhor. So sessoes finalizadas (realizadas ou canceladas) entram no
     * denominador — uma ainda AGENDADO nao aconteceu nem foi desistida
     * ainda, entao nao diz nada sobre a taxa. `quantidadeMinima` filtra
     * quem ainda nao tem amostra suficiente, mesmo raciocinio do ranking
     * de avaliacoes.
     */
    @Transactional(readOnly = true)
    public List<LinhaCancelamentoProfessor> taxaCancelamentoPorProfessor(int dias, long quantidadeMinima) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();
        List<LinhaSessaoPersonalFinalizada> sessoes = repository.sessoesFinalizadasDesde(desde);

        Map<Long, List<LinhaSessaoPersonalFinalizada>> porProfessor = sessoes.stream()
                .collect(Collectors.groupingBy(LinhaSessaoPersonalFinalizada::professorId));

        return porProfessor.entrySet().stream()
                .map(entry -> linhaCancelamento(entry.getValue()))
                .filter(linha -> linha.totalSessoes() >= quantidadeMinima)
                .sorted(Comparator.comparing(LinhaCancelamentoProfessor::taxaCancelamento).reversed())
                .toList();
    }

    private LinhaCancelamentoProfessor linhaCancelamento(List<LinhaSessaoPersonalFinalizada> sessoes) {
        long total = sessoes.size();
        long emCimaDaHora = sessoes.stream().filter(this::foiCanceladaEmCimaDaHora).count();
        BigDecimal taxa = total > 0
                ? BigDecimal.valueOf(emCimaDaHora * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LinhaSessaoPersonalFinalizada primeira = sessoes.get(0);
        return new LinhaCancelamentoProfessor(
                primeira.professorId(), primeira.professorNome(), total, emCimaDaHora, taxa);
    }

    /** Canceladas antes desta coluna existir tem canceladoEm nulo — antecedencia desconhecida, nao "em cima da hora". */
    private boolean foiCanceladaEmCimaDaHora(LinhaSessaoPersonalFinalizada sessao) {
        if (sessao.status() != StatusAgendamento.CANCELADO || sessao.canceladoEm() == null) {
            return false;
        }
        return Duration.between(sessao.canceladoEm(), sessao.dataHora()).toHours()
                < LIMITE_HORAS_CANCELAMENTO_EM_CIMA_DA_HORA;
    }

    /**
     * Taxa de ocupacao da agenda de personal: horas disponiveis (a soma dos
     * blocos de HorarioProfessor, escalada pro periodo) contra horas
     * efetivamente ocupadas por sessoes realizadas. Do menos ocupado pro
     * mais ocupado — agenda ociosa e capacidade que sobra sem uso, o
     * oposto do que a taxa de cancelamento mede.
     *
     * So entra professor com pelo menos um HorarioProfessor cadastrado:
     * sem disponibilidade configurada, nao ha contra o que comparar.
     */
    @Transactional(readOnly = true)
    public List<LinhaOcupacaoPersonal> ocupacaoPorProfessor(int dias) {
        Map<Long, List<HorarioProfessor>> horariosPorProfessor = horarioProfessorRepository.findAll().stream()
                .collect(Collectors.groupingBy(h -> h.getProfessor().getId()));

        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();
        Map<Long, Long> minutosOcupados = repository.minutosOcupadosPorProfessorDesde(desde).stream()
                .collect(Collectors.toMap(LinhaMinutosOcupadosProfessor::professorId,
                        LinhaMinutosOcupadosProfessor::minutosOcupados));

        double semanas = dias / 7.0;

        return horariosPorProfessor.entrySet().stream()
                .map(entry -> linhaOcupacao(entry.getKey(), entry.getValue(), minutosOcupados, semanas))
                .sorted(Comparator.comparing(LinhaOcupacaoPersonal::taxaOcupacao))
                .toList();
    }

    private LinhaOcupacaoPersonal linhaOcupacao(Long professorId, List<HorarioProfessor> blocos,
                                                Map<Long, Long> minutosOcupados, double semanas) {
        long minutosSemanais = blocos.stream()
                .mapToLong(h -> Duration.between(h.getHoraInicio(), h.getHoraFim()).toMinutes())
                .sum();

        BigDecimal horasDisponiveis = BigDecimal.valueOf(minutosSemanais * semanas / 60.0)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal horasOcupadas = BigDecimal.valueOf(minutosOcupados.getOrDefault(professorId, 0L) / 60.0)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal taxa = horasDisponiveis.compareTo(BigDecimal.ZERO) > 0
                ? horasOcupadas.multiply(BigDecimal.valueOf(100)).divide(horasDisponiveis, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new LinhaOcupacaoPersonal(
                professorId, blocos.get(0).getProfessor().getNome(), horasDisponiveis, horasOcupadas, taxa);
    }

    /**
     * Blocos de 30 minutos, por unidade e dia da semana, sem nenhum
     * professor cobrindo — a lacuna bruta da agenda, diferente da taxa de
     * ocupacao (que so olha professores que ja tem horario cadastrado).
     * Uma unidade sem nenhum HorarioProfessor aparece com o horario
     * comercial inteiro como lacuna, dia a dia.
     */
    @Transactional(readOnly = true)
    public List<LinhaCoberturaHorario> coberturaHorario() {
        Map<Long, List<HorarioProfessor>> horariosPorUnidade = horarioProfessorRepository.buscarTodosComUnidade()
                .stream()
                .collect(Collectors.groupingBy(h -> h.getUnidade().getId()));

        List<LinhaCoberturaHorario> lacunas = new ArrayList<>();
        for (Unidade unidade : unidadeRepository.findAll()) {
            List<HorarioProfessor> blocosDaUnidade = horariosPorUnidade.getOrDefault(unidade.getId(), List.of());
            for (DiaSemana dia : DiaSemana.values()) {
                acumularLacunasDoDia(unidade, dia, blocosDaUnidade, lacunas);
            }
        }

        return lacunas.stream()
                .sorted(Comparator.comparing(LinhaCoberturaHorario::unidadeNome)
                        .thenComparing(LinhaCoberturaHorario::diaSemana)
                        .thenComparing(LinhaCoberturaHorario::horaInicio))
                .toList();
    }

    private void acumularLacunasDoDia(Unidade unidade, DiaSemana dia, List<HorarioProfessor> blocosDaUnidade,
                                       List<LinhaCoberturaHorario> lacunas) {
        List<HorarioProfessor> blocosDoDia = blocosDaUnidade.stream()
                .filter(h -> h.getDiaSemana() == dia)
                .toList();

        for (LocalTime cursor = INICIO_HORARIO_COMERCIAL; cursor.isBefore(FIM_HORARIO_COMERCIAL);
             cursor = cursor.plusMinutes(DURACAO_BLOCO_COBERTURA_MINUTOS)) {
            LocalTime inicioSlot = cursor;
            LocalTime fimSlot = inicioSlot.plusMinutes(DURACAO_BLOCO_COBERTURA_MINUTOS);
            boolean coberto = blocosDoDia.stream()
                    .anyMatch(h -> !h.getHoraInicio().isAfter(inicioSlot) && h.getHoraFim().isAfter(inicioSlot));
            if (!coberto) {
                lacunas.add(new LinhaCoberturaHorario(unidade.getId(), unidade.getNome(), dia, inicioSlot, fimSlot));
            }
        }
    }

    /** Mesma checagem de AulaGrupoService.garantirSemConflito, do outro lado da agenda do professor. */
    private void garantirSemConflito(Long professorId, LocalDateTime inicio, LocalDateTime fim) {
        boolean cruzaPersonal = repository.findByProfessorIdAndStatus(professorId, StatusAgendamento.AGENDADO)
                .stream()
                .anyMatch(existente -> existente.getDataHora().isBefore(fim) && inicio.isBefore(existente.getFim()));
        if (cruzaPersonal) {
            throw new RegraNegocioException("O professor ja tem uma sessao de personal marcada neste horario.");
        }

        boolean cruzaAula = aulaGrupoRepository.findByProfessorIdAndStatus(professorId, StatusAula.ATIVA).stream()
                .anyMatch(existente -> existente.getDataHora().isBefore(fim) && inicio.isBefore(existente.getFim()));
        if (cruzaAula) {
            throw new RegraNegocioException("O professor ja tem uma aula em grupo marcada neste horario.");
        }
    }

    private String vazioComoNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }
}
