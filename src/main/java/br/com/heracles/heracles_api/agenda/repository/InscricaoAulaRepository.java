package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoAulaRepository extends JpaRepository<InscricaoAula, Long> {

    Optional<InscricaoAula> findByAulaIdAndAlunoIdAndStatus(Long aulaId, Long alunoId, StatusInscricao status);

    long countByAulaIdAndStatus(Long aulaId, StatusInscricao status);

    @EntityGraph(attributePaths = {"aula", "aula.professor", "aula.unidade"})
    List<InscricaoAula> findByAlunoIdAndStatusOrderByAula_DataHoraAsc(Long alunoId, StatusInscricao status);
}
