package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Treino;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TreinoRepository extends JpaRepository<Treino, Long> {
}
