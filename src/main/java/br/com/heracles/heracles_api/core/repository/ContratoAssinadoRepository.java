package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.ContratoAssinado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratoAssinadoRepository extends JpaRepository<ContratoAssinado, Long> {

    Optional<ContratoAssinado> findByAlunoId(Long alunoId);
}
