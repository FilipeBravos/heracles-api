package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    String GRAFO_COMPLETO = "select a from Assinatura a";

    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    @Query(GRAFO_COMPLETO)
    Page<Assinatura> buscarPaginadoCompleto(Pageable pageable);

    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    Optional<Assinatura> findWithAlunoAndPlanoById(Long id);

    /**
     * A matricula vigente do aluno — a que a recepcao consulta.
     *
     * "Vigente" e tudo que nao foi cancelado, INADIMPLENTE inclusive: o
     * aluno continua matriculado, so esta em atraso. O indice parcial
     * garante que so exista uma.
     */
    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    @Query("select a from Assinatura a where a.aluno.id = :alunoId and a.status <> 'CANCELADA'")
    Optional<Assinatura> buscarVigentePorAluno(Long alunoId);

    @EntityGraph(attributePaths = {"aluno", "plano"})
    @Query("select a from Assinatura a where a.aluno.id = :alunoId order by a.dataInicio desc")
    List<Assinatura> historicoDoAluno(Long alunoId);

    boolean existsByTokenParceiroAndStatusNot(String tokenParceiro, StatusAssinatura status);

    long countByStatus(StatusAssinatura status);

    long countByDataInicioGreaterThanEqual(LocalDate data);

    /**
     * Vigentes que ja passaram do vencimento: o aluno ainda consta como
     * matriculado, mas a data diz que o acesso caiu. E a fila de cobranca
     * do dia.
     */
    @Query("""
            select count(a) from Assinatura a
            where a.status <> 'CANCELADA' and a.dataVencimento < :hoje
            """)
    long contarVencidas(LocalDate hoje);
}
