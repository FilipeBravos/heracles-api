package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import br.com.heracles.heracles_api.agenda.dto.LinhaFaltaAluno;
import br.com.heracles.heracles_api.agenda.dto.LinhaPresencaBruta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InscricaoAulaRepository extends JpaRepository<InscricaoAula, Long> {

    Optional<InscricaoAula> findByAulaIdAndAlunoIdAndStatus(Long aulaId, Long alunoId, StatusInscricao status);

    Optional<InscricaoAula> findByAulaIdAndAlunoIdAndStatusIn(Long aulaId, Long alunoId, List<StatusInscricao> status);

    boolean existsByAulaIdAndAlunoIdAndStatusIn(Long aulaId, Long alunoId, List<StatusInscricao> status);

    long countByAulaIdAndStatus(Long aulaId, StatusInscricao status);

    /** A fila de espera de uma aula, do primeiro ao ultimo — quem chegou antes sobe primeiro. */
    List<InscricaoAula> findByAulaIdAndStatusOrderByInscritoEmAsc(Long aulaId, StatusInscricao status);

    /** O roster de uma aula, em ordem alfabetica — a lista que o professor confere pra marcar presenca. */
    @EntityGraph(attributePaths = "aluno")
    List<InscricaoAula> findByAulaIdAndStatusOrderByAluno_NomeAsc(Long aulaId, StatusInscricao status);

    @EntityGraph(attributePaths = {"aula", "aula.professor", "aula.unidade"})
    List<InscricaoAula> findByAlunoIdAndStatusOrderByAula_DataHoraAsc(Long alunoId, StatusInscricao status);

    /** Quantas presencas foram confirmadas (compareceu ou faltou) no periodo. */
    @Query("select count(i) from InscricaoAula i where i.presente is not null and i.aula.dataHora >= :desde")
    long totalConfirmadasDesde(LocalDateTime desde);

    /** Quantas faltas foram confirmadas no periodo. */
    @Query("select count(i) from InscricaoAula i where i.presente = false and i.aula.dataHora >= :desde")
    long totalFaltasDesde(LocalDateTime desde);

    /**
     * Faltas e presencas por aluno no periodo, do que mais falta pro que
     * menos — so entra quem tem ao menos uma falta. O Pageable so limita
     * o tamanho da lista (top N); a ordenacao e a do proprio JPQL.
     */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaFaltaAluno(
                       a.id, a.nome,
                       sum(case when i.presente = false then 1 else 0 end),
                       sum(case when i.presente = true then 1 else 0 end))
            from InscricaoAula i join i.aluno a
            where i.presente is not null and i.aula.dataHora >= :desde
            group by a.id, a.nome
            having sum(case when i.presente = false then 1 else 0 end) > 0
            order by sum(case when i.presente = false then 1 else 0 end) desc
            """)
    List<LinhaFaltaAluno> faltasPorAlunoDesde(LocalDateTime desde, Pageable pageable);

    /**
     * Presencas confirmadas no periodo, com o suficiente pra agrupar por
     * horario recorrente no servico — nao ha coluna de dia-da-semana na
     * aula, cada ocorrencia e uma linha propria, entao o agrupamento por
     * (nome, unidade, dia da semana, hora) acontece em Java, nao aqui.
     */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaPresencaBruta(
                       au.nome, un.id, un.nome, p.nome, au.dataHora, i.presente)
            from InscricaoAula i join i.aula au join au.unidade un join au.professor p
            where i.presente is not null and au.dataHora >= :desde
            """)
    List<LinhaPresencaBruta> presencasComDetalheDesde(LocalDateTime desde);
}
