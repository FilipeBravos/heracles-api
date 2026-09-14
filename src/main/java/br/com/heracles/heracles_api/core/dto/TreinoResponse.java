package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.Treino;

import java.util.List;

/** Ficha completa, com os exercicios na ordem prescrita. */
public record TreinoResponse(
        Long id,
        String nome,
        String foco,
        String nivel,
        List<ExercicioResponse> exercicios
) {

    public static TreinoResponse de(Treino treino) {
        return new TreinoResponse(
                treino.getId(),
                treino.getNome(),
                treino.getFoco(),
                treino.getNivel(),
                treino.getExercicios().stream().map(ExercicioResponse::de).toList()
        );
    }

    public record ExercicioResponse(
            Long id,
            String nome,
            String repeticoes,
            String observacoes,
            Integer ordem
    ) {
        public static ExercicioResponse de(Exercicio exercicio) {
            return new ExercicioResponse(
                    exercicio.getId(),
                    exercicio.getNome(),
                    exercicio.getRepeticoes(),
                    exercicio.getObservacoes(),
                    exercicio.getOrdem()
            );
        }
    }
}
