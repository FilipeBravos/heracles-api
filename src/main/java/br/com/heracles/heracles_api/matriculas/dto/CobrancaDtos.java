package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Cobranca;
import br.com.heracles.heracles_api.matriculas.domain.FormaPagamento;
import br.com.heracles.heracles_api.matriculas.domain.StatusCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CobrancaDtos {

    private CobrancaDtos() {
    }

    /** Uma linha do extrato de cobrancas de uma assinatura. */
    public record Response(
            Long id,
            BigDecimal valor,
            FormaPagamento formaPagamento,
            String codigoSimulado,
            LocalDate dataVencimento,
            StatusCobranca status,
            LocalDate dataPagamento
    ) {
        public static Response de(Cobranca cobranca) {
            return new Response(
                    cobranca.getId(),
                    cobranca.getValor(),
                    cobranca.getFormaPagamento(),
                    cobranca.getCodigoSimulado(),
                    cobranca.getDataVencimento(),
                    cobranca.getStatus(),
                    cobranca.getDataPagamento()
            );
        }
    }
}
