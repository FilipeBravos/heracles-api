package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Treino;

/**
 * Treino sem a lista de exercicios.
 *
 * A tela de alunos mostra apenas os nomes das fichas, entao usar este resumo
 * evita carregar todos os exercicios de todos os treinos de todos os alunos.
 */
public record TreinoResumoResponse(
        Long id,
        String nome,
        String foco,
        String nivel
) {
    public static TreinoResumoResponse de(Treino treino) {
        return new TreinoResumoResponse(
                treino.getId(),
                treino.getNome(),
                treino.getFoco(),
                treino.getNivel()
        );
    }
}
