package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaMinutosOcupadosProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessaoPersonalFinalizada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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

    /**
     * Nota media por professor, do melhor pro pior — so quem ja foi
     * avaliado entra, e so quem tem pelo menos `minimo` avaliacoes: sem
     * o HAVING, um professor com uma unica sessao de nota baixa (ou alta)
     * dominaria a ponta do ranking sem amostra nenhuma para sustentar.
     */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor(
                       p.id, p.nome, avg(s.notaAvaliacao), count(s))
            from AgendamentoPersonal s join s.professor p
            where s.notaAvaliacao is not null
            group by p.id, p.nome
            having count(s) >= :minimo
            order by avg(s.notaAvaliacao) desc
            """)
    List<LinhaAvaliacaoProfessor> mediaAvaliacaoPorProfessor(long minimo);

    /**
     * Sessoes ja finalizadas (realizadas ou canceladas) desde uma data — a
     * classificacao de "cancelamento em cima da hora" e feita em Java, a
     * partir de dataHora e canceladoEm, mesmo raciocinio de
     * AulaGrupoService.relatorioNoShowPorHorario.
     */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaSessaoPersonalFinalizada(
                       p.id, p.nome, s.status, s.dataHora, s.canceladoEm)
            from AgendamentoPersonal s join s.professor p
            where s.status in (br.com.heracles.heracles_api.agenda.domain.StatusAgendamento.REALIZADA,
                               br.com.heracles.heracles_api.agenda.domain.StatusAgendamento.CANCELADO)
              and s.dataHora >= :desde
            """)
    List<LinhaSessaoPersonalFinalizada> sessoesFinalizadasDesde(LocalDateTime desde);

    /** Minutos de sessao realizada por professor desde uma data — a ocupacao efetiva da agenda. */
    @Query("""
            select new br.com.heracles.heracles_api.agenda.dto.LinhaMinutosOcupadosProfessor(
                       p.id, sum(s.duracaoMinutos))
            from AgendamentoPersonal s join s.professor p
            where s.status = br.com.heracles.heracles_api.agenda.domain.StatusAgendamento.REALIZADA
              and s.dataHora >= :desde
            group by p.id
            """)
    List<LinhaMinutosOcupadosProfessor> minutosOcupadosPorProfessorDesde(LocalDateTime desde);
}
