package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Listagem paginada trazendo os treinos numa consulta so.
     *
     * Com @ManyToMany LAZY e um @EntityGraph explicito, no lugar do EAGER
     * que carregava treinos e exercicios de todo mundo em toda chamada.
     */
    @EntityGraph(attributePaths = {"treinos", "planoEscolhido"})
    @Query("select u from Usuario u")
    Page<Usuario> buscarPaginadoComTreinos(Pageable pageable);

    @EntityGraph(attributePaths = {"treinos", "planoEscolhido"})
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

    List<Usuario> findByTipoPerfilAndStatus(TipoPerfil tipoPerfil, StatusUsuario status);

    /** Quem a notificacao de "anamnese pendente" avisa a secretaria sobre. */
    @Query("""
            select u from Usuario u
            where u.tipoPerfil = 'ALUNO' and u.status = 'ATIVO'
              and u.id not in (select a.aluno.id from Anamnese a)
            """)
    List<Usuario> buscarAlunosSemAnamnese();

    /**
     * Aniversariantes do dia, por mes e dia — nao pelo ano, que varia por
     * aluno. `date_part` e nativo do Postgres; o dialeto do Hibernate
     * repassa direto.
     */
    @Query("""
            select u from Usuario u
            where u.tipoPerfil = 'ALUNO' and u.status = 'ATIVO' and u.dataNascimento is not null
              and function('date_part', 'month', u.dataNascimento) = :mes
              and function('date_part', 'day', u.dataNascimento) = :dia
            """)
    List<Usuario> buscarAniversariantesDoDia(int mes, int dia);

    /**
     * Alunos com matricula ativa que nunca receberam uma ficha de treino —
     * pagam, mas nunca foram "recebidos" de verdade pelo treino. Mesma
     * logica de buscarAlunosSemAnamnese, mas a pergunta e sobre prescricao,
     * nao sobre o formulario de entrada.
     */
    @Query("""
            select u from Usuario u
            where u.tipoPerfil = 'ALUNO' and u.treinos is empty
              and exists (select 1 from Assinatura a where a.aluno = u and a.status = 'ATIVA')
            """)
    Page<Usuario> buscarAlunosSemFichaDeTreino(Pageable pageable);

    /** Contagem da mesma janela de buscarAlunosSemFichaDeTreino, pro cabecalho do alerta. */
    @Query("""
            select count(u) from Usuario u
            where u.tipoPerfil = 'ALUNO' and u.treinos is empty
              and exists (select 1 from Assinatura a where a.aluno = u and a.status = 'ATIVA')
            """)
    long countAlunosSemFichaDeTreino();
}
