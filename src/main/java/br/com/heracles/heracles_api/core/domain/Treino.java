package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "treinos", schema = "core")
@Getter
@Setter
public class Treino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String foco;
    private String nivel;

    // LAZY + @OrderBy: a listagem de treinos nao carrega exercicios, e quando
    // eles sao carregados vem na ordem em que o professor prescreveu.
    @OneToMany(mappedBy = "treino", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Exercicio> exercicios = new ArrayList<>();

    @ManyToMany(mappedBy = "treinos", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Usuario> usuarios = new ArrayList<>();

    public void adicionarExercicio(Exercicio exercicio) {
        exercicio.setTreino(this);
        exercicio.setOrdem(this.exercicios.size());
        this.exercicios.add(exercicio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Treino outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
