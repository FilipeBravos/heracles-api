package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Anamnese;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnamneseRepository extends JpaRepository<Anamnese, Long> {

    Optional<Anamnese> findByAlunoId(Long alunoId);

    /** "O aluno tem anamnese?" — a pergunta que libera ou barra o vinculo de ficha. */
    boolean existsByAlunoId(Long alunoId);
}
