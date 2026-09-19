package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendamentoPersonalRepository extends JpaRepository<AgendamentoPersonal, Long> {

    @EntityGraph(attributePaths = {"aluno", "professor", "unidade"})
    Page<AgendamentoPersonal> findByProfessorId(Long professorId, Pageable pageable);

    @EntityGraph(attributePaths = {"aluno", "professor", "unidade"})
    Page<AgendamentoPersonal> findByAlunoId(Long alunoId, Pageable pageable);

    @EntityGraph(attributePaths = {"aluno", "professor", "unidade"})
    Page<AgendamentoPersonal> findAllBy(Pageable pageable);

    /** Mesmo motivo de AulaGrupoRepository: sobreposicao se checa em Java. */
    List<AgendamentoPersonal> findByProfessorIdAndStatus(Long professorId, StatusAgendamento status);
}
