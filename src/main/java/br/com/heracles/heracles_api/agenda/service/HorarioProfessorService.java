package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import br.com.heracles.heracles_api.agenda.dto.HorarioProfessorDtos;
import br.com.heracles.heracles_api.agenda.repository.HorarioProfessorRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A disponibilidade semanal de cada professor — informativa, para o balcao
 * saber quando marcar personal ou aula em grupo com ele.
 */
@Service
public class HorarioProfessorService {

    private final HorarioProfessorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;

    public HorarioProfessorService(HorarioProfessorRepository repository,
                                   UsuarioRepository usuarioRepository,
                                   UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public List<HorarioProfessorDtos.Response> listar(Long professorId) {
        garantirQueEProfessor(professorId);
        return repository.findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(professorId).stream()
                .map(HorarioProfessorDtos.Response::de)
                .toList();
    }

    @Transactional
    public HorarioProfessorDtos.Response criar(Long professorId, HorarioProfessorDtos.Salvar request) {
        Usuario professor = garantirQueEProfessor(professorId);

        if (!request.horaInicio().isBefore(request.horaFim())) {
            throw new RegraNegocioException("O horario de inicio precisa ser antes do horario de fim.");
        }

        Unidade unidade = unidadeRepository.findById(request.unidadeId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", request.unidadeId()));

        boolean sobrepoe = repository.findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(professorId).stream()
                .filter(h -> h.getDiaSemana() == request.diaSemana())
                .anyMatch(h -> h.getHoraInicio().isBefore(request.horaFim())
                        && request.horaInicio().isBefore(h.getHoraFim()));
        if (sobrepoe) {
            throw new RegraNegocioException(
                    "Ja existe um horario cadastrado para %s neste dia que cruza com o informado."
                            .formatted(professor.getNome()));
        }

        HorarioProfessor horario = new HorarioProfessor();
        horario.setProfessor(professor);
        horario.setUnidade(unidade);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFim(request.horaFim());

        return HorarioProfessorDtos.Response.de(repository.save(horario));
    }

    /**
     * Confere que o bloco e mesmo do professor da rota antes de remover —
     * sem isso, o id do bloco bastaria para apagar o horario de outro
     * professor.
     */
    @Transactional
    public void remover(Long professorId, Long horarioId) {
        HorarioProfessor horario = repository.findById(horarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Horario", horarioId));
        if (!horario.getProfessor().getId().equals(professorId)) {
            throw RecursoNaoEncontradoException.de("Horario", horarioId);
        }
        repository.delete(horario);
    }

    private Usuario garantirQueEProfessor(Long professorId) {
        Usuario usuario = usuarioRepository.findById(professorId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Professor", professorId));
        if (usuario.getTipoPerfil() != TipoPerfil.PROFESSOR) {
            throw new RegraNegocioException(
                    "\"%s\" nao esta cadastrado(a) como professor.".formatted(usuario.getNome()));
        }
        return usuario;
    }
}
