package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.LinhaAlunoAnamnese;
import br.com.heracles.heracles_api.core.dto.LinhaReavaliacaoVencidaBruta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
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
     * Aniversariantes do mes corrente, do dia mais proximo pro mais
     * distante — mesmo filtro de buscarAniversariantesDoDia, agora pelo
     * mes inteiro, pra secretaria planejar com antecedencia em vez de
     * so descobrir no dia.
     */
    @Query("""
            select u from Usuario u
            where u.tipoPerfil = 'ALUNO' and u.status = 'ATIVO' and u.dataNascimento is not null
              and function('date_part', 'month', u.dataNascimento) = :mes
            order by function('date_part', 'day', u.dataNascimento) asc
            """)
    List<Usuario> buscarAniversariantesDoMes(int mes);

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

    /**
     * Alunos com matricula ativa cuja ultima avaliacao fisica passou da
     * janela, ou que nunca fizeram nenhuma — mesma logica de
     * buscarAlunosSemFichaDeTreino, agora sobre avaliacao fisica em vez
     * de ficha de treino. "Nunca fez" entra tambem, e e o caso mais grave
     * (nulls first): a subconsulta devolve nulo, nao uma data pequena.
     */
    @Query("""
            select new br.com.heracles.heracles_api.core.dto.LinhaReavaliacaoVencidaBruta(
                       u.id, u.nome, u.email, u.telefone,
                       (select max(av.data) from AvaliacaoFisica av where av.aluno = u))
            from Usuario u
            where u.tipoPerfil = 'ALUNO'
              and exists (select 1 from Assinatura a where a.aluno = u and a.status = 'ATIVA')
              and (
                (select max(av.data) from AvaliacaoFisica av where av.aluno = u) < :limite
                or not exists (select 1 from AvaliacaoFisica av where av.aluno = u)
              )
            order by (select max(av.data) from AvaliacaoFisica av where av.aluno = u) asc nulls first
            """)
    Page<LinhaReavaliacaoVencidaBruta> buscarReavaliacaoVencida(LocalDate limite, Pageable pageable);

    /** Contagem da mesma janela de buscarReavaliacaoVencida, pro cabecalho do alerta. */
    @Query("""
            select count(u) from Usuario u
            where u.tipoPerfil = 'ALUNO'
              and exists (select 1 from Assinatura a where a.aluno = u and a.status = 'ATIVA')
              and (
                (select max(av.data) from AvaliacaoFisica av where av.aluno = u) < :limite
                or not exists (select 1 from AvaliacaoFisica av where av.aluno = u)
              )
            """)
    long countReavaliacaoVencida(LocalDate limite);

    /**
     * Alunos com matricula vigente e se ja preencheram a anamnese — para a
     * cobertura de anamnese por unidade. "Vigente" aqui e qualquer status
     * exceto CANCELADA, mesmo criterio de
     * AssinaturaRepository.buscarUnidadesVigentesPorAlunos: os dois lados
     * do cruzamento por unidade precisam concordar sobre quem entra.
     */
    @Query("""
            select new br.com.heracles.heracles_api.core.dto.LinhaAlunoAnamnese(
                       u.id, case when a.id is not null then true else false end)
            from Usuario u
            left join Anamnese a on a.aluno = u
            where u.tipoPerfil = 'ALUNO'
              and exists (select 1 from Assinatura s where s.aluno = u and s.status <> 'CANCELADA')
            """)
    List<LinhaAlunoAnamnese> buscarAlunosVigentesComAnamnese();
}
