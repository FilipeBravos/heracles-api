package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma aula em grupo agendada: uma ocorrencia especifica, nao uma serie
 * recorrente. Quem quer "Spinning toda terca" cria uma aula por terca —
 * sem motor de recorrencia nesta entrega.
 */
@Entity
@Table(name = "aulas_grupo", schema = "agenda")
@Getter
@Setter
public class AulaGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

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

    @Column(name = "capacidade_maxima")
    private int capacidadeMaxima;

    @Enumerated(EnumType.STRING)
    private StatusAula status;

    /** Fim da aula, para checar sobreposicao com outros compromissos do professor. */
    public LocalDateTime getFim() {
        return dataHora.plusMinutes(duracaoMinutos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AulaGrupo outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
