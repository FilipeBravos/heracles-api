package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Treino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TreinoRepository extends JpaRepository<Treino, Long> {

    /** Ficha completa: usada na tela de detalhes e na edicao. */
    @EntityGraph(attributePaths = "exercicios")
    Optional<Treino> findWithExerciciosById(Long id);

    /**
     * Listagem com exercicios. Como Treino -> Exercicio e uma colecao,
     * a paginacao e aplicada antes do join para nao paginar em memoria.
     */
    @EntityGraph(attributePaths = "exercicios")
    @Query("select t from Treino t")
    Page<Treino> buscarPaginadoComExercicios(Pageable pageable);
}
