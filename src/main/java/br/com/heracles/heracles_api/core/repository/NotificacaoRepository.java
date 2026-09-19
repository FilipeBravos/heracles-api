package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Notificacao;
import br.com.heracles.heracles_api.core.domain.TipoNotificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioId(Long destinatarioId, Pageable pageable);

    long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    /**
     * Impede reavisar o mesmo destinatario, do mesmo tipo, sobre a mesma
     * referencia, mais de uma vez desde `desde` — os jobs rodam uma vez
     * por dia, entao `desde` = inicio do dia da mais recente basta.
     *
     * `referenciaId` nulo compara igual a nulo de proposito: e o caso do
     * resumo diario de anamnese pendente, que nao e sobre um aluno so.
     */
    @Query("""
            select case when count(n) > 0 then true else false end from Notificacao n
            where n.destinatario.id = :destinatarioId and n.tipo = :tipo
              and ((:referenciaId is null and n.referenciaId is null) or n.referenciaId = :referenciaId)
              and n.criadaEm >= :desde
            """)
    boolean existeDesde(Long destinatarioId, TipoNotificacao tipo, Long referenciaId, LocalDateTime desde);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.destinatario.id = :destinatarioId and n.lida = false")
    void marcarTodasComoLidas(Long destinatarioId);
}
