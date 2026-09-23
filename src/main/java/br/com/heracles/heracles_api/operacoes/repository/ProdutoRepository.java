package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = "unidade")
    @Query("select p from Produto p where (:apenasAtivos = false or p.ativo = true)")
    Page<Produto> buscarPaginado(boolean apenasAtivos, Pageable pageable);

    @EntityGraph(attributePaths = "unidade")
    Optional<Produto> findWithUnidadeById(Long id);

    /**
     * Carrega o produto travando a linha ate o fim da transacao (SELECT ...
     * FOR UPDATE).
     *
     * E o que impede duas vendas simultaneas do ultimo item: sem a trava,
     * as duas leem o mesmo estoque, as duas acham que ha saldo e as duas
     * gravam — vendendo o que nao existe. O CHECK (quantidade_estoque >= 0)
     * e a segunda barreira, caso alguem escreva por fora da aplicacao.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarParaVenda(Long id);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

    boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);

    /**
     * Produtos ativos abaixo do proprio estoque minimo, do maior deficit
     * pro menor — cada produto tem seu limiar agora, entao a comparacao e
     * sempre contra o campo do proprio registro, nunca um valor global.
     */
    @EntityGraph(attributePaths = "unidade")
    @Query("""
            select p from Produto p
            where p.ativo = true and p.quantidadeEstoque < p.estoqueMinimo
            order by (p.estoqueMinimo - p.quantidadeEstoque) desc
            """)
    List<Produto> buscarComEstoqueBaixo();

    /** Contagem da mesma consulta de buscarComEstoqueBaixo, pro cartao do dashboard. */
    @Query("select count(p) from Produto p where p.ativo = true and p.quantidadeEstoque < p.estoqueMinimo")
    long countComEstoqueBaixo();

    /** Candidatos ao relatorio de produtos parados — a data de corte e aplicada em Java. */
    @EntityGraph(attributePaths = "unidade")
    List<Produto> findByAtivoTrue();
}
