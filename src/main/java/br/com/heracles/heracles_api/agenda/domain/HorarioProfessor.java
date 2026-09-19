package br.com.heracles.heracles_api.agenda.domain;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Um bloco recorrente da semana em que um professor esta disponivel numa
 * unidade — informativo, para o balcao saber quando marcar personal ou
 * aula em grupo com ele. Nao valida, sozinho, se uma aula ou sessao cabe
 * dentro do bloco: isso ficaria de checagem cruzada com data especifica
 * (feriado, folga pontual) fora do escopo desta entrega.
 */
@Entity
@Table(name = "horarios_professor", schema = "agenda")
@Getter
@Setter
public class HorarioProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id")
    private Usuario professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana")
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HorarioProfessor outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
