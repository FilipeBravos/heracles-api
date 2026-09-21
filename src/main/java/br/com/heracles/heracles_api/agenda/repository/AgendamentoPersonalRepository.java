package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** Nota media por professor, do melhor pro pior — so quem ja foi avaliado entra. */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor(
                       p.id, p.nome, avg(s.notaAvaliacao), count(s))
            from AgendamentoPersonal s join s.professor p
            where s.notaAvaliacao is not null
            group by p.id, p.nome
            order by avg(s.notaAvaliacao) desc
            """)
    List<LinhaAvaliacaoProfessor> mediaAvaliacaoPorProfessor();
}
