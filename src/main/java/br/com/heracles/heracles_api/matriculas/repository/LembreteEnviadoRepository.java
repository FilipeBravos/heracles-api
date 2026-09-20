package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;
import br.com.heracles.heracles_api.matriculas.domain.LembreteEnviado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LembreteEnviadoRepository extends JpaRepository<LembreteEnviado, Long> {

    /** Ja existe lembrete deste estagio para esta assinatura? Base do "um por estagio, nunca repetido". */
    boolean existsByAssinaturaIdAndEstagio(Long assinaturaId, EstagioLembrete estagio);

    List<LembreteEnviado> findByAssinaturaIdOrderByDataEnvioDesc(Long assinaturaId);

    /** Para o relatorio de inadimplencia mostrar o ultimo lembrete de cada linha da pagina. */
    List<LembreteEnviado> findByAssinaturaIdIn(List<Long> assinaturaIds);
}
