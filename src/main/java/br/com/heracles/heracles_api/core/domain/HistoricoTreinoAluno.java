package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Um periodo em que uma ficha esteve com um aluno.
 *
 * O sincronismo de fichas (Usuario.treinos) e um retrato do agora: trocar
 * a ficha substitui a linha, sem dizer qual era a anterior nem por quanto
 * tempo o aluno a treinou. Este registro existe para o aluno ver o que já
 * treinou, nao so o que treina hoje.
 *
 * Nome, foco e nivel vêm copiados da ficha no momento da troca, e não
 * lidos de Treino ao exibir: a ficha pode ser renomeada depois, ou
 * apagada de vez. O historico descreve o que o aluno treinou naquele
 * periodo — outra pergunta da que "o que a ficha e hoje".
 */
@Entity
@Table(name = "historico_treinos_aluno", schema = "core")
@Getter
@Setter
public class HistoricoTreinoAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    /**
     * Nulo quando a ficha original foi apagada. O nome abaixo continua
     * contando a historia; so a referencia para o registro vivo se perde.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treino_id")
    private Treino treino;

    @Column(name = "treino_nome")
    private String treinoNome;

    @Column(name = "treino_foco")
    private String treinoFoco;

    @Column(name = "treino_nivel")
    private String treinoNivel;

    @Column(name = "vinculado_em")
    private LocalDateTime vinculadoEm;

    @Column(name = "desvinculado_em")
    private LocalDateTime desvinculadoEm;

    /** Fecha o periodo: a ficha deixou de ser do aluno a partir de agora. */
    public void encerrar(LocalDateTime agora) {
        this.desvinculadoEm = agora;
    }

    public boolean estaAberto() {
        return this.desvinculadoEm == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoricoTreinoAluno outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
