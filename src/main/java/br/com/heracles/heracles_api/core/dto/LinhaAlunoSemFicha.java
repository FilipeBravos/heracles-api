package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Usuario;

import java.time.LocalDateTime;

/**
 * Um aluno com matricula ativa que nunca recebeu uma ficha de treino —
 * pagou, mas nunca foi "recebido" de verdade pelo treino.
 */
public record LinhaAlunoSemFicha(
        Long alunoId,
        String alunoNome,
        String email,
        String telefone,
        LocalDateTime dataCadastro
) {
    public static LinhaAlunoSemFicha de(Usuario aluno) {
        return new LinhaAlunoSemFicha(
                aluno.getId(), aluno.getNome(), aluno.getEmail(), aluno.getTelefone(), aluno.getDataCadastro());
    }
}
