package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaCancelamentoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessaoPersonalFinalizada;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
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

    private final AgendamentoPersonalRepository repository;
    private final AulaGrupoRepository aulaGrupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;

    public AgendamentoPersonalService(AgendamentoPersonalRepository repository,
                                      AulaGrupoRepository aulaGrupoRepository,
                                      UsuarioRepository usuarioRepository,
                                      UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.aulaGrupoRepository = aulaGrupoRepository;
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
