package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;
import br.com.heracles.heracles_api.matriculas.domain.LembreteEnviado;
import br.com.heracles.heracles_api.matriculas.dto.LinhaLembreteParaEfetividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface LembreteEnviadoRepository extends JpaRepository<LembreteEnviado, Long> {

    /** Ja existe lembrete deste estagio para esta assinatura? Base do "um por estagio, nunca repetido". */
    boolean existsByAssinaturaIdAndEstagio(Long assinaturaId, EstagioLembrete estagio);

    List<LembreteEnviado> findByAssinaturaIdOrderByDataEnvioDesc(Long assinaturaId);

    /** Para o relatorio de inadimplencia mostrar o ultimo lembrete de cada linha da pagina. */
    List<LembreteEnviado> findByAssinaturaIdIn(List<Long> assinaturaIds);

    /**
     * Lembretes enviados desde uma data, com o suficiente pra avaliar a
     * conversao em pagamento em Java — o cruzamento com a cobranca mais
     * proxima do envio nao entra em `select new`.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.LinhaLembreteParaEfetividade(
                       l.assinatura.id, l.estagio, l.canal, l.dataEnvio)
            from LembreteEnviado l
            where l.dataEnvio >= :desde
            """)
    List<LinhaLembreteParaEfetividade> lembretesParaEfetividadeDesde(LocalDateTime desde);
}
