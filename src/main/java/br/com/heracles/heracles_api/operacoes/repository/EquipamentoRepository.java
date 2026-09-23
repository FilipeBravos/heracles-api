package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.Equipamento;
import br.com.heracles.heracles_api.operacoes.domain.StatusEquipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    @EntityGraph(attributePaths = "unidade")
    @Query("select e from Equipamento e")
    Page<Equipamento> buscarPaginado(Pageable pageable);

    @EntityGraph(attributePaths = "unidade")
    Optional<Equipamento> findWithUnidadeById(Long id);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

    long countByStatusAtual(StatusEquipamento status);

    /** Equipamentos com acompanhamento preventivo configurado, candidatos ao relatorio de vencidos. */
    @EntityGraph(attributePaths = "unidade")
    List<Equipamento> findByIntervaloDiasManutencaoIsNotNull();
}
