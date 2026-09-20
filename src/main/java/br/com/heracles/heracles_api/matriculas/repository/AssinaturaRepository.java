package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada;
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

    /** Quem o lembrete automatico avisa no estagio INADIMPLENTE — o dedup de LembreteEnviado evita repetir. */
    @EntityGraph(attributePaths = {"aluno", "plano"})
    List<Assinatura> findByStatus(StatusAssinatura status);

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

    /**
     * Quantas assinaturas foram canceladas em cada mes, desde a data
     * informada — mesmo formato de contarPorMesDesde, so que por
     * data_cancelamento em vez de data_inicio.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemMensal(
                       year(a.dataCancelamento), month(a.dataCancelamento), count(a))
            from Assinatura a
            where a.dataCancelamento >= :desde
            group by year(a.dataCancelamento), month(a.dataCancelamento)
            order by year(a.dataCancelamento), month(a.dataCancelamento)
            """)
    List<ContagemMensal> contarCancelamentosPorMesDesde(LocalDate desde);

    /**
     * Quantas assinaturas ja existiam, e ainda nao tinham sido canceladas,
     * antes do dia informado — a base contra a qual o churn de um mes se
     * mede. Uma cancelada exatamente no dia ainda entra: ela estava de pe
     * quando o mes comecou.
     */
    @Query("""
            select count(a) from Assinatura a
            where a.dataInicio < :inicioDoMes
            and (a.dataCancelamento is null or a.dataCancelamento >= :inicioDoMes)
            """)
    long contarAtivasEm(LocalDate inicioDoMes);

    /** Mesma base de contarAtivasEm, agrupada por plano — para o detalhamento por plano do painel de retencao. */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada(p.id, p.nome, count(a))
            from Assinatura a join a.plano p
            where a.dataInicio < :inicioDoMes
            and (a.dataCancelamento is null or a.dataCancelamento >= :inicioDoMes)
            group by p.id, p.nome
            """)
    List<ContagemAgrupada> contarAtivasPorPlanoEm(LocalDate inicioDoMes);

    /** Cancelamentos de um mes fechado (intervalo semiaberto), agrupados por plano. */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada(p.id, p.nome, count(a))
            from Assinatura a join a.plano p
            where a.dataCancelamento >= :inicio and a.dataCancelamento < :fimExclusivo
            group by p.id, p.nome
            """)
    List<ContagemAgrupada> contarCancelamentosPorPlano(LocalDate inicio, LocalDate fimExclusivo);

    /**
     * Mesma base de contarAtivasEm, agrupada por unidade.
     *
     * O join com plano.unidades espalha cada assinatura por todas as
     * unidades que o plano dela cobre: um plano de rede que perde um
     * aluno conta como perda em cada unidade que cobria, nao numa so —
     * a assinatura nunca pertenceu a uma unidade so.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada(u.id, u.nome, count(a))
            from Assinatura a join a.plano.unidades u
            where a.dataInicio < :inicioDoMes
            and (a.dataCancelamento is null or a.dataCancelamento >= :inicioDoMes)
            group by u.id, u.nome
            """)
    List<ContagemAgrupada> contarAtivasPorUnidadeEm(LocalDate inicioDoMes);

    /** Cancelamentos de um mes fechado, agrupados por unidade — mesmo espalhamento de contarAtivasPorUnidadeEm. */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada(u.id, u.nome, count(a))
            from Assinatura a join a.plano.unidades u
            where a.dataCancelamento >= :inicio and a.dataCancelamento < :fimExclusivo
            group by u.id, u.nome
            """)
    List<ContagemAgrupada> contarCancelamentosPorUnidade(LocalDate inicio, LocalDate fimExclusivo);

    /**
     * Ranking do programa de indicacao: quantas matriculas cada aluno
     * trouxe, canceladas inclusive — a indicacao aconteceu de qualquer
     * jeito, e descontar a cancelada penalizaria quem indicou por um
     * motivo que nao e dele.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada(
                       i.id, i.nome, count(a))
            from Assinatura a join a.indicadoPor i
            group by i.id, i.nome
            order by count(a) desc
            """)
    List<ContagemAgrupada> contarIndicacoesPorAluno();
}
