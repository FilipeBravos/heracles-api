package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.ComissaoIndicacao;
import br.com.heracles.heracles_api.matriculas.domain.StatusComissao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ComissaoIndicacaoDtos {

    private ComissaoIndicacaoDtos() {
    }

    /** Cabecalho do alerta: quantas comissoes esperam aprovacao da secretaria. */
    public record Resumo(long pendentes) {
    }

    public record Response(
            Long id,
            Long indicadorId,
            String indicadorNome,
            Long indicadoId,
            String indicadoNome,
            BigDecimal valor,
            StatusComissao status,
            LocalDateTime dataCriacao,
            LocalDateTime dataResolucao
    ) {
        public static Response de(ComissaoIndicacao comissao) {
            return new Response(
                    comissao.getId(),
                    comissao.getIndicador().getId(),
                    comissao.getIndicador().getNome(),
                    comissao.getAssinatura().getAluno().getId(),
                    comissao.getAssinatura().getAluno().getNome(),
                    comissao.getValor(),
                    comissao.getStatus(),
                    comissao.getDataCriacao(),
                    comissao.getDataResolucao());
        }
    }
}
