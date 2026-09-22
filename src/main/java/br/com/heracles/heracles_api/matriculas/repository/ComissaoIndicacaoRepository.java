package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.ComissaoIndicacao;
import br.com.heracles.heracles_api.matriculas.domain.StatusComissao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComissaoIndicacaoRepository extends JpaRepository<ComissaoIndicacao, Long> {

    /** Ja existe comissao para esta assinatura? Base do "no maximo uma por indicacao". */
    boolean existsByAssinaturaId(Long assinaturaId);

    @EntityGraph(attributePaths = {"indicador", "assinatura", "assinatura.aluno"})
    Page<ComissaoIndicacao> findByStatus(StatusComissao status, Pageable pageable);

    long countByStatus(StatusComissao status);
}
