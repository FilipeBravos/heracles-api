package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Listagem paginada trazendo os treinos numa consulta so.
     *
     * Com @ManyToMany LAZY e um @EntityGraph explicito, no lugar do EAGER
     * que carregava treinos e exercicios de todo mundo em toda chamada.
     */
    @EntityGraph(attributePaths = "treinos")
    @Query("select u from Usuario u")
    Page<Usuario> buscarPaginadoComTreinos(Pageable pageable);

    @EntityGraph(attributePaths = "treinos")
    Optional<Usuario> findWithTreinosById(Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatus(StatusUsuario status);

    long countByDataCadastroAfter(LocalDateTime momento);

    @Query("select count(distinct u.id) from Usuario u where size(u.treinos) > 0")
    long contarUsuariosComTreino();
}
