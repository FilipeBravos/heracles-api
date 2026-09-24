package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import br.com.heracles.heracles_api.core.dto.LinhaExecucaoParaAdesao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ExecucaoExercicioRepository extends JpaRepository<ExecucaoExercicio, Long> {

    /** A evolucao de um exercicio especifico do aluno, do mais recente pro mais antigo. */
    List<ExecucaoExercicio> findByAlunoIdAndExercicioIdOrderByDataExecucaoDesc(Long alunoId, Long exercicioId);

    /**
     * Execucoes desde uma data, com a prescricao do exercicio ainda
     * vinculado — o join com exercicio (nao exercicioNome) deixa de fora
     * quem foi removido da ficha, ja que sem o exercicio nao ha mais
     * series/repeticoes minimas pra comparar.
     */
    @Query("""
            select new br.com.heracles.heracles_api.core.dto.LinhaExecucaoParaAdesao(
                       a.id, a.nome, ex.series, ex.repeticoesMin, e.seriesRealizadas, e.repeticoesRealizadas)
            from ExecucaoExercicio e join e.aluno a join e.exercicio ex
            where e.dataExecucao >= :desde
            """)
    List<LinhaExecucaoParaAdesao> execucoesParaAdesaoDesde(LocalDate desde);
}
