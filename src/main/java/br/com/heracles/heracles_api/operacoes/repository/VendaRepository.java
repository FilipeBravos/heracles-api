package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}
