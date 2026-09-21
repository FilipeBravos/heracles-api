package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.Venda;
import br.com.heracles.heracles_api.operacoes.dto.LinhaFaturamentoPorUnidade;
import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoMaisVendido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = {"unidade", "operador", "aluno", "itens", "itens.produto"})
    @Query("select v from Venda v")
    Page<Venda> buscarPaginadoCompleto(Pageable pageable);

    @EntityGraph(attributePaths = {"unidade", "operador", "aluno", "itens", "itens.produto"})
    Optional<Venda> findWithItensById(Long id);

    /** Faturamento do periodo. COALESCE para devolver zero em vez de nulo. */
    @Query("select coalesce(sum(v.valorTotal), 0) from Venda v where v.dataVenda >= :inicio")
    BigDecimal faturamentoDesde(LocalDateTime inicio);

    long countByDataVendaAfter(LocalDateTime momento);

    /**
     * Produtos mais vendidos no periodo, por quantidade — do mais vendido
     * pro menos. O Pageable so limita o tamanho da lista (top N); a
     * ordenacao e a do proprio JPQL, entao um Pageable sem sort a preserva.
     */
    @Query("""
            select new br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoMaisVendido(
                       p.id, p.nome, sum(i.quantidade), sum(i.precoUnitario * i.quantidade))
            from Venda v join v.itens i join i.produto p
            where v.dataVenda >= :desde
            group by p.id, p.nome
            order by sum(i.quantidade) desc
            """)
    List<LinhaProdutoMaisVendido> produtosMaisVendidosDesde(LocalDateTime desde, Pageable pageable);

    /** Faturamento e numero de vendas por unidade no periodo, da maior receita pra menor. */
    @Query("""
            select new br.com.heracles.heracles_api.operacoes.dto.LinhaFaturamentoPorUnidade(
                       u.id, u.nome, coalesce(sum(v.valorTotal), 0), count(v))
            from Venda v join v.unidade u
            where v.dataVenda >= :desde
            group by u.id, u.nome
            order by sum(v.valorTotal) desc
            """)
    List<LinhaFaturamentoPorUnidade> faturamentoPorUnidadeDesde(LocalDateTime desde);
}
