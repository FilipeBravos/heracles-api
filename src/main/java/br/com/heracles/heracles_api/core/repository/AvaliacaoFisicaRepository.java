package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    List<AvaliacaoFisica> findByAlunoIdOrderByDataDesc(Long alunoId);
}
