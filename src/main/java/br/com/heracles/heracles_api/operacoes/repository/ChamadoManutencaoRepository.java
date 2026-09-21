package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.ChamadoManutencao;
import br.com.heracles.heracles_api.operacoes.domain.StatusChamado;
import br.com.heracles.heracles_api.operacoes.dto.LinhaEquipamentoProblematico;
import br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPorUnidade;
import br.com.heracles.heracles_api.operacoes.dto.LinhaTempoResolucao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChamadoManutencaoRepository extends JpaRepository<ChamadoManutencao, Long> {

    List<ChamadoManutencao> findByEquipamentoIdOrderByDataChamadoDesc(Long equipamentoId);

    Optional<ChamadoManutencao> findByEquipamentoIdAndStatus(Long equipamentoId, StatusChamado status);

    long countByDataChamadoAfter(LocalDateTime desde);

    long countByDataChamadoAfterAndStatus(LocalDateTime desde, StatusChamado status);

    @Query("select coalesce(sum(c.custoReparo), 0) from ChamadoManutencao c " +
            "where c.status = 'RESOLVIDO' and c.dataChamado >= :desde")
    BigDecimal custoTotalDesde(LocalDateTime desde);

    /** Instantes de abertura e resolucao dos chamados resolvidos no periodo, para o tempo medio no servico. */
    @Query("select new br.com.heracles.heracles_api.operacoes.dto.LinhaTempoResolucao(c.dataChamado, c.dataResolucao) " +
            "from ChamadoManutencao c where c.status = 'RESOLVIDO' and c.dataChamado >= :desde")
    List<LinhaTempoResolucao> temposResolucaoDesde(LocalDateTime desde);

    /**
     * Equipamentos com mais chamados no periodo, do mais problematico pro
     * menos. O Pageable so limita o tamanho da lista (top N); a ordenacao
     * e a do proprio JPQL.
     */
    @Query("""
            select new br.com.heracles.heracles_api.operacoes.dto.LinhaEquipamentoProblematico(
                       e.id, e.nome, u.nome, count(c), coalesce(sum(c.custoReparo), 0))
            from ChamadoManutencao c join c.equipamento e join e.unidade u
            where c.dataChamado >= :desde
            group by e.id, e.nome, u.nome
            order by count(c) desc
            """)
    List<LinhaEquipamentoProblematico> equipamentosProblematicosDesde(LocalDateTime desde, Pageable pageable);

    /** Chamados e custo de manutencao por unidade no periodo, do maior custo pro menor. */
    @Query("""
            select new br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPorUnidade(
                       u.id, u.nome, count(c), coalesce(sum(c.custoReparo), 0))
            from ChamadoManutencao c join c.equipamento e join e.unidade u
            where c.dataChamado >= :desde
            group by u.id, u.nome
            order by coalesce(sum(c.custoReparo), 0) desc
            """)
    List<LinhaManutencaoPorUnidade> manutencaoPorUnidadeDesde(LocalDateTime desde);
}
