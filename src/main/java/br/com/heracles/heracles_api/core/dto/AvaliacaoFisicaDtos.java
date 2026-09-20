package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
            BigDecimal circunferenciaPeito,

            @Size(max = 1000, message = "Observacoes muito longas")
            String observacoes,

            /** Frente, lado, costas — o professor decide quantas e quais angulos. */
            @Size(max = 6, message = "No maximo 6 fotos por avaliacao")
            List<@Valid Foto> fotos
    ) {
    }

    /** Uma foto da galeria da avaliacao, em base64 puro (sem o prefixo "data:image/...;base64,"). */
    public record Foto(
            @NotBlank(message = "Foto sem conteudo")
            String base64,

            @NotBlank(message = "Informe o tipo da foto")
            @Pattern(regexp = "image/(jpeg|png|webp)", message = "A foto precisa ser JPEG, PNG ou WebP")
            String contentType
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
            BigDecimal circunferenciaPeito,
            String observacoes,
            /** Ids das fotos da galeria, na ordem de upload — cada uma se busca em .../fotos/{fotoId}. */
            List<Long> fotoIds,
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
                    avaliacao.getCircunferenciaPeito(),
                    avaliacao.getObservacoes(),
                    avaliacao.getFotos().stream().map(AvaliacaoFisicaFoto::getId).toList(),
                    avaliacao.getDataCriacao()
            );
        }
    }

    /**
     * A diferenca entre duas avaliacoes, campo a campo (mais recente menos
     * mais antiga). Nulo quando falta a medida em um dos dois lados — a
     * tela mostra "sem dado" em vez de um numero inventado.
     */
    public record Delta(
            BigDecimal pesoKg,
            BigDecimal alturaCm,
            BigDecimal imc,
            BigDecimal percentualGordura,
            BigDecimal circunferenciaCintura,
            BigDecimal circunferenciaQuadril,
            BigDecimal circunferenciaBraco,
            BigDecimal circunferenciaCoxa,
            BigDecimal circunferenciaPeito
    ) {
        static Delta de(Response de, Response para) {
            return new Delta(
                    subtrair(para.pesoKg(), de.pesoKg()),
                    subtrair(para.alturaCm(), de.alturaCm()),
                    subtrair(para.imc(), de.imc()),
                    subtrair(para.percentualGordura(), de.percentualGordura()),
                    subtrair(para.circunferenciaCintura(), de.circunferenciaCintura()),
                    subtrair(para.circunferenciaQuadril(), de.circunferenciaQuadril()),
                    subtrair(para.circunferenciaBraco(), de.circunferenciaBraco()),
                    subtrair(para.circunferenciaCoxa(), de.circunferenciaCoxa()),
                    subtrair(para.circunferenciaPeito(), de.circunferenciaPeito())
            );
        }

        private static BigDecimal subtrair(BigDecimal maisRecente, BigDecimal maisAntiga) {
            return (maisRecente == null || maisAntiga == null) ? null : maisRecente.subtract(maisAntiga);
        }
    }

    /**
     * O comparativo entre duas avaliacoes — a mais antiga e a mais recente
     * por padrao, ou duas escolhidas.
     *
     * `disponivel: false` quando o aluno tem menos de duas avaliacoes: nao
     * ha o que comparar ainda, e isso e normal, nao erro — mesma ideia de
     * `AnamneseDtos.Response.ausente()`.
     */
    public record Comparativo(
            boolean disponivel,
            Response de,
            Response para,
            Delta delta
    ) {
        public static Comparativo indisponivel() {
            return new Comparativo(false, null, null, null);
        }

        public static Comparativo de(AvaliacaoFisica de, AvaliacaoFisica para) {
            Response respostaDe = Response.de(de);
            Response respostaPara = Response.de(para);
            return new Comparativo(true, respostaDe, respostaPara, Delta.de(respostaDe, respostaPara));
        }
    }
}
