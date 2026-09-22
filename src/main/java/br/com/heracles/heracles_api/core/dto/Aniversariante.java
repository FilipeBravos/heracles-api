package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Usuario;

import java.time.LocalDate;

/** Uma linha do painel de aniversariantes do mes. */
public record Aniversariante(Long alunoId, String alunoNome, LocalDate dataNascimento, String telefone, String email) {
    public static Aniversariante de(Usuario aluno) {
        return new Aniversariante(
                aluno.getId(), aluno.getNome(), aluno.getDataNascimento(), aluno.getTelefone(), aluno.getEmail());
    }
}
