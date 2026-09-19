package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
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

import java.time.LocalDateTime;

/**
 * Sessoes de personal: agendadas pela secretaria/administracao a pedido do
 * aluno, nunca self-service — diferente da aula em grupo, personal
 * continua um atendimento marcado por gente.
 */
@Service
public class AgendamentoPersonalService {

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
        sessao.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoPersonalDtos.Response.de(sessao);
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
