package br.com.heracles.heracles_api.operacoes.repository;

import br.com.heracles.heracles_api.operacoes.domain.ChamadoManutencao;
import br.com.heracles.heracles_api.operacoes.domain.StatusChamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChamadoManutencaoRepository extends JpaRepository<ChamadoManutencao, Long> {

    List<ChamadoManutencao> findByEquipamentoIdOrderByDataChamadoDesc(Long equipamentoId);

    Optional<ChamadoManutencao> findByEquipamentoIdAndStatus(Long equipamentoId, StatusChamado status);
}
