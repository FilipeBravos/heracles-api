package br.com.heracles.heracles_api.matriculas.repository;

import br.com.heracles.heracles_api.matriculas.domain.Checkin;
import br.com.heracles.heracles_api.matriculas.dto.LinhaOcupacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    /** A ordem vem do Pageable — o controller ja define "momento desc" por padrao. */
    @EntityGraph(attributePaths = {"aluno", "unidade"})
    Page<Checkin> findByAlunoId(Long alunoId, Pageable pageable);

    /**
     * Ocupacao por hora do dia, por unidade: quantos check-ins liberados
     * desde a data informada.
     *
     * So liberado entra — quem foi barrado (vencido, inadimplente) nao
     * ocupou espaco nem equipamento, so tentou. Somar por hora do dia (nao
     * por dia da semana) responde a pergunta que a gestao faz: em que
     * horario a casa costuma lotar, pra dimensionar equipamento e horario
     * de aula em grupo.
     */
    @Query("""
            select new br.com.heracles.heracles_api.matriculas.dto.LinhaOcupacao(
                       u.id, u.nome, hour(c.momento), count(c))
            from Checkin c join c.unidade u
            where c.liberado = true and c.momento >= :desde
            group by u.id, u.nome, hour(c.momento)
            """)
    List<LinhaOcupacao> contarOcupacaoPorUnidadeEHora(LocalDateTime desde);
}
