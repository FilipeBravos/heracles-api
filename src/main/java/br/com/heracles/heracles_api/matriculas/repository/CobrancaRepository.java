package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Cobranca;
import br.com.heracles.heracles_api.matriculas.domain.StatusCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByAssinaturaIdAndStatus(Long assinaturaId, StatusCobranca status);

    List<Cobranca> findByAssinaturaIdOrderByDataVencimentoDesc(Long assinaturaId);

    /** Usado para anexar a cobranca em aberto de cada linha do relatorio de inadimplencia, sem N+1. */
    List<Cobranca> findByAssinaturaIdInAndStatus(List<Long> assinaturaIds, StatusCobranca status);
}
