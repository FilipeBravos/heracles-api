package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.Cobranca;
import br.com.heracles.heracles_api.matriculas.domain.FormaPagamento;
import br.com.heracles.heracles_api.matriculas.domain.MotivoAcesso;
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
            LocalDate dataInicio,

            @NotNull(message = "Informe a forma de pagamento")
            FormaPagamento formaPagamento
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
            FormaPagamento formaPagamento,
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
                    assinatura.getFormaPagamento(),
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

    /**
     * Um ponto do grafico de matriculas por mes.
     *
     * `mes` sai como `yyyy-MM`: chave estavel e sem ambiguidade. O rotulo
     * legivel ("set/26") e formatado no front, que tem o locale — a API
     * nao precisa saber em que idioma a tela esta.
     */
    public record PontoMensal(
            String mes,
            long quantidade
    ) {
    }

    /**
     * A serie inteira, sem buracos.
     *
     * `total` e a soma do periodo, que o cabecalho do painel mostra sem
     * obrigar o leitor a somar as barras de cabeca.
     */
    public record HistoricoMensal(
            int meses,
            long total,
            List<PontoMensal> pontos
    ) {
    }

    /**
     * Um mes da serie de churn.
     *
     * `ativosNoInicio` e quem ja existia (e nao tinha sido cancelada)
     * antes do mes comecar — a base contra a qual o cancelamento do mes
     * se mede. `taxaChurn` vem pronta (0 quando a base e zero, para o
     * front nao ter que tratar divisao por zero).
     */
    public record PontoChurn(
            String mes,
            long ativosNoInicio,
            long cancelados,
            double taxaChurn
    ) {
    }

    public record HistoricoChurn(
            int meses,
            List<PontoChurn> pontos
    ) {
    }

    /** Uma linha do detalhamento de churn por plano ou por unidade, no mes de referencia. */
    public record LinhaChurn(
            Long id,
            String nome,
            long ativosNoInicio,
            long cancelados,
            double taxaChurn
    ) {
    }

    /**
     * O painel de retencao: a tendencia mensal e o detalhamento do ultimo
     * mes fechado — o mes corrente fica de fora do detalhamento por
     * estar incompleto, e mostraria uma taxa artificialmente baixa.
     */
    public record Retencao(
            HistoricoChurn historico,
            String mesReferencia,
            List<LinhaChurn> porPlano,
            List<LinhaChurn> porUnidade
    ) {
    }

    /**
     * Uma linha do relatorio de inadimplencia.
     *
     * `diasParaVencer` usa a mesma convencao de `Vencimento` (negativo
     * quando ja venceu) de proposito: e a mesma classificacao de regua
     * que a tela de Matriculas ja faz no cliente a partir de
     * status+vencida+diasParaVencer, e as duas telas precisam ler o
     * mesmo aluno do mesmo jeito.
     *
     * `cobrancaPendenteId`/`formaPagamento`/`codigoSimulado` saem nulos
     * quando nao ha cobranca em aberto (por exemplo, logo apos cancelar
     * a assinatura) — a tela nao oferece "confirmar pagamento" nesse caso.
     */
    public record LinhaInadimplencia(
            Long assinaturaId,
            Long alunoId,
            String alunoNome,
            String planoNome,
            BigDecimal valorMensal,
            LocalDate dataVencimento,
            StatusAssinatura status,
            boolean vencida,
            long diasParaVencer,
            Long cobrancaPendenteId,
            FormaPagamento formaPagamento,
            String codigoSimulado
    ) {
        public static LinhaInadimplencia de(Assinatura assinatura, Cobranca cobrancaPendente, LocalDate hoje) {
            return new LinhaInadimplencia(
                    assinatura.getId(),
                    assinatura.getAluno().getId(),
                    assinatura.getAluno().getNome(),
                    assinatura.getPlano().getNome(),
                    assinatura.getPlano().getValorMensal(),
                    assinatura.getDataVencimento(),
                    assinatura.getStatus(),
                    assinatura.estaVencidaEm(hoje),
                    ChronoUnit.DAYS.between(hoje, assinatura.getDataVencimento()),
                    cobrancaPendente != null ? cobrancaPendente.getId() : null,
                    cobrancaPendente != null ? cobrancaPendente.getFormaPagamento() : null,
                    cobrancaPendente != null ? cobrancaPendente.getCodigoSimulado() : null
            );
        }
    }

    /**
     * Contagem por etapa da regua, para o cabecalho do relatorio.
     *
     * Nao inclui "em dia": um relatorio de inadimplencia nao precisa
     * dizer quantos alunos nao pedem nenhuma acao.
     */
    public record ResumoInadimplencia(
            long venceEmBreve,
            long vencidas,
            long inadimplentes
    ) {
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
