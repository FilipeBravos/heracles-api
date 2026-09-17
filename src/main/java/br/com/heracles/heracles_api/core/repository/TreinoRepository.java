package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Treino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TreinoRepository extends JpaRepository<Treino, Long> {

    /** Ficha completa: usada na tela de detalhes e na edicao. */
    @EntityGraph(attributePaths = "exercicios")
    Optional<Treino> findWithExerciciosById(Long id);

    /**
     * As fichas vinculadas a um aluno, com os exercicios.
     *
     * O id vem sempre de quem esta autenticado, nunca da requisicao —
     * quem chama e o controller da area do aluno, que o tira do token.
     */
    @EntityGraph(attributePaths = "exercicios")
    @Query("select t from Treino t join t.usuarios u where u.id = :alunoId order by t.nome asc")
    List<Treino> fichasDoAluno(Long alunoId);

    /**
     * Listagem com exercicios. Como Treino -> Exercicio e uma colecao,
     * a paginacao e aplicada antes do join para nao paginar em memoria.
     */
    @EntityGraph(attributePaths = "exercicios")
    @Query("select t from Treino t")
    Page<Treino> buscarPaginadoComExercicios(Pageable pageable);
}
