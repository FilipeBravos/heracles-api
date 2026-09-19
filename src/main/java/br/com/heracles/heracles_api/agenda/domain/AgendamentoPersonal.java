package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma sessao de personal (1:1) entre aluno e professor.
 *
 * Diferente da aula em grupo, nao e self-service: quem agenda e a
 * secretaria, no balcao, a pedido do aluno — personal continua um
 * atendimento marcado por gente, nao uma vaga que se reserva sozinho.
 */
@Entity
@Table(name = "sessoes_personal", schema = "agenda")
@Getter
@Setter
public class AgendamentoPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id")
    private Usuario professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @Column(name = "duracao_minutos")
    private int duracaoMinutos;

    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    public LocalDateTime getFim() {
        return dataHora.plusMinutes(duracaoMinutos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendamentoPersonal outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
