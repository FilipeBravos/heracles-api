package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioProfessorRepository extends JpaRepository<HorarioProfessor, Long> {

    @EntityGraph(attributePaths = {"unidade"})
    List<HorarioProfessor> findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(Long professorId);
}
