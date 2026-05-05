package br.com.heracles.heracles_api.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

    @Entity
    @Table(name = "exercicios", schema = "core")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class Exercicio {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String nome;
        private String repeticoes;
        private String observacoes;

        @ManyToOne
        @JoinColumn(name = "treino_id")
        @JsonIgnore // Evita um loop infinito quando o Java for transformar isso em JSON
        private Treino treino;
    }

