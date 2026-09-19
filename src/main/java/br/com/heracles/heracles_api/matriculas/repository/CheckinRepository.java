package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    /** A ordem vem do Pageable — o controller ja define "momento desc" por padrao. */
    @EntityGraph(attributePaths = {"aluno", "unidade"})
    Page<Checkin> findByAlunoId(Long alunoId, Pageable pageable);
}
