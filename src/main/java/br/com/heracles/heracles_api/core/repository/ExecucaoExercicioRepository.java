package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecucaoExercicioRepository extends JpaRepository<ExecucaoExercicio, Long> {

    /** A evolucao de um exercicio especifico do aluno, do mais recente pro mais antigo. */
    List<ExecucaoExercicio> findByAlunoIdAndExercicioIdOrderByDataExecucaoDesc(Long alunoId, Long exercicioId);
}
