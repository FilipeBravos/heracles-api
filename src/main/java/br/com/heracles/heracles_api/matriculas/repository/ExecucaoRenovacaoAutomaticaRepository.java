package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.ExecucaoRenovacaoAutomatica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoRenovacaoAutomaticaRepository extends JpaRepository<ExecucaoRenovacaoAutomatica, Long> {

    Page<ExecucaoRenovacaoAutomatica> findAllByOrderByDataExecucaoDesc(Pageable pageable);
}
