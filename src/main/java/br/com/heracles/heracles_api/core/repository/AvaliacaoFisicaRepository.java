package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    List<AvaliacaoFisica> findByAlunoIdOrderByDataDesc(Long alunoId);

    /** As pontas do historico, para o comparativo padrao (primeira x mais recente). */
    Optional<AvaliacaoFisica> findFirstByAlunoIdOrderByDataAsc(Long alunoId);

    Optional<AvaliacaoFisica> findFirstByAlunoIdOrderByDataDesc(Long alunoId);
}
