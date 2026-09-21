package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Cobranca;
import br.com.heracles.heracles_api.matriculas.domain.StatusCobranca;
import br.com.heracles.heracles_api.matriculas.dto.SomaAgrupada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByAssinaturaIdAndStatus(Long assinaturaId, StatusCobranca status);

    List<Cobranca> findByAssinaturaIdOrderByDataVencimentoDesc(Long assinaturaId);

    /** Usado para anexar a cobranca em aberto de cada linha do relatorio de inadimplencia, sem N+1. */
    List<Cobranca> findByAssinaturaIdInAndStatus(List<Long> assinaturaIds, StatusCobranca status);

    /**
     * Dinheiro parado: cobrancas pendentes de quem ja esta vencido ou
     * inadimplente. Mesma regua de buscarEmAtencao, so que em R$ em vez de
     * contagem — "vence em breve" fica de fora porque ainda nao e atraso.
     */
    @Query("""
            select coalesce(sum(c.valor), 0) from Cobranca c
            where c.status = 'PENDENTE'
            and (c.assinatura.status = 'INADIMPLENTE'
                 or (c.assinatura.status = 'ATIVA' and c.assinatura.dataVencimento < :hoje))
            """)
    BigDecimal somarInadimplenciaEmAberto(LocalDate hoje);

    /**
     * Mesma base de somarInadimplenciaEmAberto, agrupada por unidade —
     * mesmo espalhamento das consultas de AssinaturaRepository: uma
     * cobranca de plano de rede conta inteira em cada unidade que o plano
     * cobre.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.SomaAgrupada(u.id, u.nome, coalesce(sum(c.valor), 0))
            from Cobranca c join c.assinatura.plano.unidades u
            where c.status = 'PENDENTE'
            and (c.assinatura.status = 'INADIMPLENTE'
                 or (c.assinatura.status = 'ATIVA' and c.assinatura.dataVencimento < :hoje))
            group by u.id, u.nome
            """)
    List<SomaAgrupada> somarInadimplenciaEmAbertoPorUnidade(LocalDate hoje);

    /**
     * Previsao de caixa do mes: cobrancas com vencimento dentro do
     * periodo, pagas ou ainda pendentes — canceladas ficam de fora, pois
     * esse dinheiro nunca vai entrar.
     */
    @Query("""
            select coalesce(sum(c.valor), 0) from Cobranca c
            where c.status <> 'CANCELADA' and c.dataVencimento >= :inicio and c.dataVencimento <= :fim
            """)
    BigDecimal somarCobrancasNoPeriodo(LocalDate inicio, LocalDate fim);
}
