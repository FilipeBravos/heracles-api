package br.com.heracles.heracles_api.agenda.repository;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AulaGrupoRepository extends JpaRepository<AulaGrupo, Long> {

    @EntityGraph(attributePaths = {"professor", "unidade"})
    Page<AulaGrupo> findByStatusAndDataHoraGreaterThanEqual(StatusAula status, LocalDateTime desde, Pageable pageable);

    /**
     * Para o conflito de agenda: todos os compromissos ativos do professor,
     * checados um a um contra a nova janela de horario em Java — a
     * sobreposicao depende de duracao (dataHora + duracaoMinutos), que nao
     * e coluna, entao nao da para expressar como query derivada.
     */
    List<AulaGrupo> findByProfessorIdAndStatus(Long professorId, StatusAula status);
}
