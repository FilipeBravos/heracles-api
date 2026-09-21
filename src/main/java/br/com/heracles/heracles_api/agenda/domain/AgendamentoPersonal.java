package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
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

    /** Preenchida pelo aluno depois da sessao realizada — nula ate ele avaliar. */
    @Column(name = "nota_avaliacao")
    private Integer notaAvaliacao;

    @Column(name = "comentario_avaliacao")
    private String comentarioAvaliacao;

    public LocalDateTime getFim() {
        return dataHora.plusMinutes(duracaoMinutos);
    }

    /**
     * So o professor que esteve na sessao confirma que ela aconteceu — a
     * secretaria agenda e cancela, mas nao estava la para atestar.
     * So depois disso o aluno pode avaliar.
     */
    public void marcarRealizada(LocalDateTime agora) {
        if (this.status != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Esta sessao nao esta agendada.");
        }
        if (agora.isBefore(getFim())) {
            throw new RegraNegocioException("Esta sessao ainda nao aconteceu.");
        }
        this.status = StatusAgendamento.REALIZADA;
    }

    /**
     * Cancelar so faz sentido antes da sessao acontecer — depois de
     * REALIZADA (e possivelmente avaliada), desfazer o registro apagaria
     * um atendimento que de fato ocorreu.
     */
    public void cancelar() {
        if (this.status == StatusAgendamento.REALIZADA) {
            throw new RegraNegocioException("Esta sessao ja foi realizada e nao pode ser cancelada.");
        }
        this.status = StatusAgendamento.CANCELADO;
    }

    /** O aluno avalia uma vez so — a nota registrada fica, nao e uma media que se refaz. */
    public void avaliar(int nota, String comentario) {
        if (this.status != StatusAgendamento.REALIZADA) {
            throw new RegraNegocioException("Esta sessao ainda nao foi realizada.");
        }
        if (this.notaAvaliacao != null) {
            throw new RegraNegocioException("Esta sessao ja foi avaliada.");
        }
        this.notaAvaliacao = nota;
        this.comentarioAvaliacao = comentario;
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
