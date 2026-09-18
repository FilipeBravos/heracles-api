package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A anamnese de um aluno.
 *
 * Existir e a resposta a "o aluno preencheu a anamnese?" — nao os valores
 * dos campos individuais. Um aluno saudavel legitimamente deixa
 * condicoesSaude, lesoesCirurgias, medicamentosUso e restricoesMedicas em
 * branco; exigir que estivessem preenchidos obrigaria a escrever "Nenhuma"
 * em cada um so para a regra aceitar. A linha existir e que conta.
 *
 * Por isso vive numa tabela a parte, com aluno_id UNIQUE, e nao como
 * colunas em Usuario: "tem anamnese" e uma pergunta de existencia
 * (`AnamneseRepository.existsByAlunoId`), simples e sem ambiguidade.
 */
@Entity
@Table(name = "anamneses", schema = "core")
@Getter
@Setter
public class Anamnese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    private String objetivo;

    @Column(name = "condicoes_saude")
    private String condicoesSaude;

    @Column(name = "lesoes_cirurgias")
    private String lesoesCirurgias;

    @Column(name = "medicamentos_uso")
    private String medicamentosUso;

    @Column(name = "restricoes_medicas")
    private String restricoesMedicas;

    @Column(name = "contato_emergencia_nome")
    private String contatoEmergenciaNome;

    @Column(name = "contato_emergencia_telefone")
    private String contatoEmergenciaTelefone;

    @Column(name = "preenchida_em")
    private LocalDateTime preenchidaEm;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Anamnese outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
