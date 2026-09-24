package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import br.com.heracles.heracles_api.core.dto.LinhaAvaliacaoParaEvolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    List<AvaliacaoFisica> findByAlunoIdOrderByDataDesc(Long alunoId);

    /** As pontas do historico, para o comparativo padrao (primeira x mais recente). */
    Optional<AvaliacaoFisica> findFirstByAlunoIdOrderByDataAsc(Long alunoId);

    Optional<AvaliacaoFisica> findFirstByAlunoIdOrderByDataDesc(Long alunoId);

    /**
     * Avaliacoes desde uma data, com o suficiente pra calcular o delta por
     * aluno em Java — subtracao e media nao entram em `select new`.
     * Ordenada por aluno e data para que a primeira e a ultima avaliacao de
     * cada aluno sejam, respectivamente, o primeiro e o ultimo item do
     * grupo dele.
     */
    @Query("""
            select new br.com.heracles.heracles_api.core.dto.LinhaAvaliacaoParaEvolucao(
                       a.aluno.id, a.data, a.pesoKg, a.alturaCm, a.percentualGordura)
            from AvaliacaoFisica a
            where a.data >= :desde
            order by a.aluno.id, a.data asc
            """)
    List<LinhaAvaliacaoParaEvolucao> avaliacoesParaEvolucaoDesde(LocalDate desde);
}
