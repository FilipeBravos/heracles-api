package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ExecucaoExercicioDtos {

    private ExecucaoExercicioDtos() {
    }

    public record Request(
            @NotNull(message = "Informe o exercicio")
            Long exercicioId,

            @NotNull(message = "Informe a data")
            @PastOrPresent(message = "A data nao pode ser no futuro")
            LocalDate dataExecucao,

            @NotNull(message = "Informe as series realizadas")
            @Min(value = 1, message = "Informe pelo menos 1 serie")
            @Max(value = 50, message = "Quantidade de series invalida")
            Integer seriesRealizadas,

            @NotNull(message = "Informe as repeticoes realizadas")
            @Min(value = 1, message = "Informe pelo menos 1 repeticao")
            @Max(value = 500, message = "Quantidade de repeticoes invalida")
            Integer repeticoesRealizadas,

            /** Nulo para exercicio sem carga externa (ex: abdominal ate a falha). */
            @DecimalMin(value = "0.00", message = "A carga nao pode ser negativa")
            @Digits(integer = 4, fraction = 2, message = "Carga invalida")
            BigDecimal cargaRealizada,

            @Size(max = 500, message = "Observacao longa demais")
            String observacao
    ) {
    }

    public record Response(
            Long id,
            Long exercicioId,
            String exercicioNome,
            LocalDate dataExecucao,
            Integer seriesRealizadas,
            Integer repeticoesRealizadas,
            BigDecimal cargaRealizada,
            String observacao
    ) {
        public static Response de(ExecucaoExercicio execucao) {
            return new Response(
                    execucao.getId(),
                    execucao.getExercicio() != null ? execucao.getExercicio().getId() : null,
                    execucao.getExercicioNome(),
                    execucao.getDataExecucao(),
                    execucao.getSeriesRealizadas(),
                    execucao.getRepeticoesRealizadas(),
                    execucao.getCargaRealizada(),
                    execucao.getObservacao()
            );
        }
    }
}
