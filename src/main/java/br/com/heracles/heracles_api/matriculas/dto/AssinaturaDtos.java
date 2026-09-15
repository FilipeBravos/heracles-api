package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class AssinaturaDtos {

    private AssinaturaDtos() {
    }

    public record Matricular(
            @NotNull(message = "Informe o aluno")
            Long alunoId,

            @NotNull(message = "Informe o plano")
            Long planoId,

            @NotNull(message = "Informe a origem da matricula")
            OrigemAssinatura origem,

            @Size(max = 100, message = "Token do parceiro longo demais")
            String tokenParceiro,

            /** Ausente vale hoje — a matricula de balcao comeca no dia. */
            LocalDate dataInicio
    ) {
        // A data de vencimento nao entra aqui: ela e calculada a partir do
        // periodo do plano. Se viesse do cliente, bastaria editar a
        // requisicao para se dar um ano de academia.
    }

    public record Response(
            Long id,
            Long alunoId,
            String alunoNome,
            Long planoId,
            String planoNome,
            BigDecimal valorMensal,
            OrigemAssinatura origem,
            String tokenParceiro,
            LocalDate dataInicio,
            LocalDate dataVencimento,
            StatusAssinatura status,
            LocalDate dataCancelamento,
            /** Derivado da data, nao gravado: o status e o que o operador marcou. */
            boolean vencida
    ) {
        public static Response de(Assinatura assinatura) {
            return de(assinatura, LocalDate.now());
        }

        public static Response de(Assinatura assinatura, LocalDate hoje) {
            return new Response(
                    assinatura.getId(),
                    assinatura.getAluno().getId(),
                    assinatura.getAluno().getNome(),
                    assinatura.getPlano().getId(),
                    assinatura.getPlano().getNome(),
                    assinatura.getPlano().getValorMensal(),
                    assinatura.getOrigem(),
                    assinatura.getTokenParceiro(),
                    assinatura.getDataInicio(),
                    assinatura.getDataVencimento(),
                    assinatura.getStatus(),
                    assinatura.getDataCancelamento(),
                    assinatura.estaVencidaEm(hoje)
            );
        }
    }

    /** Por que o acesso foi liberado ou barrado — o front decide a cor com isto, nao com o texto. */
    public enum MotivoAcesso {
        LIBERADO,
        SEM_MATRICULA,
        INADIMPLENTE,
        VENCIDA,
        UNIDADE_NAO_COBERTA
    }

    /**
     * Veredito da catraca.
     *
     * Devolve o motivo junto: barrar sem dizer por que obriga a recepcao a
     * abrir a ficha do aluno para descobrir se e atraso, vencimento ou
     * plano que nao cobre a unidade — tres encaminhamentos diferentes.
     */
    public record Acesso(
            boolean liberado,
            MotivoAcesso motivo,
            String mensagem,
            Response assinatura
    ) {
    }
}
