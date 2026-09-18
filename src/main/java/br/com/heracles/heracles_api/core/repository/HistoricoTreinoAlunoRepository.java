package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HistoricoTreinoAlunoRepository extends JpaRepository<HistoricoTreinoAluno, Long> {

    /** O periodo em aberto de um treino especifico com um aluno, se houver. */
    @Query("""
            select h from HistoricoTreinoAluno h
            where h.aluno.id = :alunoId and h.treino.id = :treinoId and h.desvinculadoEm is null
            """)
    Optional<HistoricoTreinoAluno> buscarAbertoPorAlunoETreino(Long alunoId, Long treinoId);

    /**
     * Todos os periodos em aberto de um treino, entre todos os alunos.
     *
     * Usado quando a ficha e apagada de vez: o vinculo desaparece de
     * Usuario.treinos sem passar por sincronizarTreinos, entao ninguem mais
     * fecharia estes periodos se o service de exclusao nao o fizer.
     */
    @Query("select h from HistoricoTreinoAluno h where h.treino.id = :treinoId and h.desvinculadoEm is null")
    List<HistoricoTreinoAluno> buscarAbertosPorTreino(Long treinoId);

    /**
     * O historico do aluno: fichas que ja nao sao mais dele.
     *
     * A que esta em aberto (desvinculado_em nulo) e a atual, e "Meu treino"
     * ja a mostra — repeti-la aqui seria a mesma ficha em dois lugares com
     * nomes diferentes ("atual" e "anterior") para a mesma coisa.
     */
    @Query("""
            select h from HistoricoTreinoAluno h
            where h.aluno.id = :alunoId and h.desvinculadoEm is not null
            order by h.desvinculadoEm desc
            """)
    List<HistoricoTreinoAluno> historicoDoAluno(Long alunoId);
}
