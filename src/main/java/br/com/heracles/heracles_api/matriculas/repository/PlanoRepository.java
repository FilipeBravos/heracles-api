package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Plano;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlanoRepository extends JpaRepository<Plano, Long> {

    /**
     * As unidades vem juntas porque a listagem as mostra em toda linha —
     * sem o grafo, seria uma consulta por plano da pagina.
     */
    @EntityGraph(attributePaths = "unidades")
    @Query("select p from Plano p where (:apenasAtivos = false or p.ativo = true)")
    Page<Plano> buscarPaginado(boolean apenasAtivos, Pageable pageable);

    @EntityGraph(attributePaths = "unidades")
    Optional<Plano> findWithUnidadesById(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
