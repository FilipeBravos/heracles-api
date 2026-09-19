package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AvaliacaoFisicaDtos {

    private AvaliacaoFisicaDtos() {
    }

    public record Salvar(
            /** Ausente vale hoje. */
            LocalDate data,

            @NotNull(message = "Informe o peso") @Positive(message = "Peso invalido")
            BigDecimal pesoKg,

            @NotNull(message = "Informe a altura") @Positive(message = "Altura invalida")
            BigDecimal alturaCm,

            @DecimalMin(value = "0", message = "Percentual de gordura invalido")
            @DecimalMax(value = "100", message = "Percentual de gordura invalido")
            BigDecimal percentualGordura,

            BigDecimal circunferenciaCintura,
            BigDecimal circunferenciaQuadril,
            BigDecimal circunferenciaBraco,
            BigDecimal circunferenciaCoxa,

            @Size(max = 1000, message = "Observacoes muito longas")
            String observacoes,

            /** Base64 puro, sem o prefixo "data:image/...;base64,". Opcional. */
            String fotoBase64,

            @Pattern(regexp = "image/(jpeg|png|webp)", message = "A foto precisa ser JPEG, PNG ou WebP")
            String fotoContentType
    ) {
    }

    public record Response(
            Long id,
            LocalDate data,
            BigDecimal pesoKg,
            BigDecimal alturaCm,
            BigDecimal imc,
            BigDecimal percentualGordura,
            BigDecimal circunferenciaCintura,
            BigDecimal circunferenciaQuadril,
            BigDecimal circunferenciaBraco,
            BigDecimal circunferenciaCoxa,
            String observacoes,
            boolean temFoto,
            LocalDateTime dataCriacao
    ) {
        public static Response de(AvaliacaoFisica avaliacao) {
            return new Response(
                    avaliacao.getId(),
                    avaliacao.getData(),
                    avaliacao.getPesoKg(),
                    avaliacao.getAlturaCm(),
                    avaliacao.calcularImc(),
                    avaliacao.getPercentualGordura(),
                    avaliacao.getCircunferenciaCintura(),
                    avaliacao.getCircunferenciaQuadril(),
                    avaliacao.getCircunferenciaBraco(),
                    avaliacao.getCircunferenciaCoxa(),
                    avaliacao.getObservacoes(),
                    avaliacao.getFoto() != null,
                    avaliacao.getDataCriacao()
            );
        }
    }
}
