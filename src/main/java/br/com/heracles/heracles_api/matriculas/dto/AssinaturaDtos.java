package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    /**
     * Linha da fila de vencimentos.
     *
     * `diasParaVencer` vem calculado daqui, e nao do navegador: o dia de
     * hoje do servidor e o mesmo que decide `vencida` e o veredito da
     * catraca. Se o front contasse, um relogio adiantado faria a tela
     * discordar da catraca sobre o mesmo aluno.
     */
    public record Vencimento(
            Long assinaturaId,
            Long alunoId,
            String alunoNome,
            String planoNome,
            LocalDate dataVencimento,
            StatusAssinatura status,
            /** Negativo quando ja venceu. */
            long diasParaVencer
    ) {
        public static Vencimento de(Assinatura assinatura, LocalDate hoje) {
            return new Vencimento(
                    assinatura.getId(),
                    assinatura.getAluno().getId(),
                    assinatura.getAluno().getNome(),
                    assinatura.getPlano().getNome(),
                    assinatura.getDataVencimento(),
                    assinatura.getStatus(),
                    ChronoUnit.DAYS.between(hoje, assinatura.getDataVencimento())
            );
        }
    }

    /**
     * A fila inteira em numero, so um pedaco em lista.
     *
     * `total` existe para o painel poder dizer "e mais 14": sem ele, uma
     * lista truncada em oito parece a fila completa, e e exatamente nesse
     * ponto que o trabalho some de vista.
     */
    public record FilaDeVencimentos(
            int dias,
            long total,
            List<Vencimento> itens
    ) {
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
