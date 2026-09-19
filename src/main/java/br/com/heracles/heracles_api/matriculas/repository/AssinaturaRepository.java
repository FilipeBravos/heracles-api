package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.dto.ContagemMensal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    String GRAFO_COMPLETO = "select a from Assinatura a";

    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    @Query(GRAFO_COMPLETO)
    Page<Assinatura> buscarPaginadoCompleto(Pageable pageable);

    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    Optional<Assinatura> findWithAlunoAndPlanoById(Long id);

    /**
     * A matricula vigente do aluno — a que a recepcao consulta.
     *
     * "Vigente" e tudo que nao foi cancelado, INADIMPLENTE inclusive: o
     * aluno continua matriculado, so esta em atraso. O indice parcial
     * garante que so exista uma.
     */
    @EntityGraph(attributePaths = {"aluno", "plano", "plano.unidades"})
    @Query("select a from Assinatura a where a.aluno.id = :alunoId and a.status <> 'CANCELADA'")
    Optional<Assinatura> buscarVigentePorAluno(Long alunoId);

    @EntityGraph(attributePaths = {"aluno", "plano"})
    @Query("select a from Assinatura a where a.aluno.id = :alunoId order by a.dataInicio desc")
    List<Assinatura> historicoDoAluno(Long alunoId);

    /**
     * Fila de vencimentos: vigentes que vencem ate a data limite.
     *
     * O <= alcanca tambem as ja vencidas, de proposito. Uma matricula que
     * venceu ontem e mais urgente que uma que vence amanha — deixa-la de
     * fora faria o caso mais grave sumir da tela justamente por ser grave
     * demais. A ordem crescente coloca o atraso maior no topo.
     */
    @EntityGraph(attributePaths = {"aluno", "plano"})
    @Query("""
            select a from Assinatura a
            where a.status <> 'CANCELADA' and a.dataVencimento <= :limite
            order by a.dataVencimento asc, a.id asc
            """)
    List<Assinatura> vencendoAte(LocalDate limite, Pageable pageable);

    @Query("""
            select count(a) from Assinatura a
            where a.status <> 'CANCELADA' and a.dataVencimento <= :limite
            """)
    long contarVencendoAte(LocalDate limite);

    /**
     * Quantas matriculas comecaram em cada mes, desde a data informada.
     *
     * Conta tudo por `dataInicio`, canceladas inclusive: uma matricula que
     * foi cancelada depois ainda aconteceu naquele mes. Descontar as
     * canceladas reescreveria o passado a cada cancelamento, e o grafico
     * de um mes fechado mudaria sozinho.
     *
     * Meses sem matricula simplesmente nao voltam desta consulta — quem
     * preenche o zero e o servico, senao o eixo do tempo mentiria.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemMensal(
                       year(a.dataInicio), month(a.dataInicio), count(a))
            from Assinatura a
            where a.dataInicio >= :desde
            group by year(a.dataInicio), month(a.dataInicio)
            order by year(a.dataInicio), month(a.dataInicio)
            """)
    List<ContagemMensal> contarPorMesDesde(LocalDate desde);

    boolean existsByTokenParceiroAndStatusNot(String tokenParceiro, StatusAssinatura status);

    long countByStatus(StatusAssinatura status);

    long countByDataInicioGreaterThanEqual(LocalDate data);

    /**
     * Vigentes que ja passaram do vencimento: o aluno ainda consta como
     * matriculado, mas a data diz que o acesso caiu. E a fila de cobranca
     * do dia.
     */
    @Query("""
            select count(a) from Assinatura a
            where a.status <> 'CANCELADA' and a.dataVencimento < :hoje
            """)
    long contarVencidas(LocalDate hoje);

    /**
     * A regua de cobranca: quem ja foi marcado inadimplente, ou ainda
     * esta ativo mas vence dentro da janela (vence-em-breve e ja vencida
     * inclusas, pelo mesmo motivo de `vencendoAte`).
     *
     * Nao inclui CANCELADA nem quem esta em dia fora da janela — essas
     * nao pedem nenhuma acao da secretaria, e um relatorio de
     * inadimplencia que lista todo mundo deixa de ser um relatorio.
     */
    @EntityGraph(attributePaths = {"aluno", "plano"})
    @Query("""
            select a from Assinatura a
            where a.status = 'INADIMPLENTE'
               or (a.status = 'ATIVA' and a.dataVencimento <= :limiteDaJanela)
            """)
    Page<Assinatura> buscarEmAtencao(LocalDate limiteDaJanela, Pageable pageable);

    @Query("select count(a) from Assinatura a where a.status = 'ATIVA' and a.dataVencimento < :hoje")
    long countAtivasVencidas(LocalDate hoje);

    @Query("""
            select count(a) from Assinatura a
            where a.status = 'ATIVA' and a.dataVencimento >= :hoje and a.dataVencimento <= :limiteDaJanela
            """)
    long countAtivasVencendoEntre(LocalDate hoje, LocalDate limiteDaJanela);

    /** Mesma janela de countAtivasVencendoEntre, mas com as linhas — usado para gerar os avisos de vencimento. */
    @EntityGraph(attributePaths = {"aluno", "plano"})
    @Query("""
            select a from Assinatura a
            where a.status = 'ATIVA' and a.dataVencimento >= :hoje and a.dataVencimento <= :limiteDaJanela
            """)
    List<Assinatura> buscarAtivasVencendoEntre(LocalDate hoje, LocalDate limiteDaJanela);

    /**
     * Quem o job diario vai marcar INADIMPLENTE: ativa, mas vencida havia
     * mais dias do que a tolerancia permite.
     */
    @Query("select a from Assinatura a where a.status = 'ATIVA' and a.dataVencimento < :limite")
    List<Assinatura> buscarAtivasVencidasAntesDe(LocalDate limite);
}
