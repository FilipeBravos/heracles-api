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
        List<ExercicioResponse> exercicios,
        /** Repeticoes totais minimas da ficha — soma de series x repeticoesMin. */
        int volumePrescritoMinimo
) {

    public static TreinoResponse de(Treino treino) {
        List<ExercicioResponse> exercicios = treino.getExercicios().stream()
                .map(ExercicioResponse::de)
                .toList();

        return new TreinoResponse(
                treino.getId(),
                treino.getNome(),
                treino.getFoco(),
                treino.getNivel(),
                exercicios,
                exercicios.stream().mapToInt(e -> e.series() * e.repeticoesMin()).sum()
        );
    }

    public record ExercicioResponse(
            Long id,
            String nome,
            Integer series,
            Integer repeticoesMin,
            Integer repeticoesMax,
            String carga,
            String observacoes,
            Integer ordem
    ) {
        public static ExercicioResponse de(Exercicio exercicio) {
            return new ExercicioResponse(
                    exercicio.getId(),
                    exercicio.getNome(),
                    exercicio.getSeries(),
                    exercicio.getRepeticoesMin(),
                    exercicio.getRepeticoesMax(),
                    exercicio.getCarga(),
                    exercicio.getObservacoes(),
                    exercicio.getOrdem()
            );
        }
    }
}
