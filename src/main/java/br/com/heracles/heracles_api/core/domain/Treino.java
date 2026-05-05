package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "treinos", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Treino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String foco;
    private String nivel;

    // Trazemos de volta a lista de exercícios para a Ficha mostrar os detalhes
    @OneToMany(mappedBy = "treino", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Exercicio> exercicios = new ArrayList<>();

    // O JsonIgnore correto para o Angular não entrar em loop com os usuários
    @ManyToMany(mappedBy = "treinos")
    @JsonIgnore
    private List<Usuario> usuarios = new ArrayList<>();

}
