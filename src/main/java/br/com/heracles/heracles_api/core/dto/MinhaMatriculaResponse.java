package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.TipoCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A matricula como o proprio aluno a ve.
 *
 * Nao e o Response de matriculas encurtado: as perguntas sao outras. O
 * balcao pergunta de quem e a matricula (alunoId, alunoNome) e por qual
 * codigo de parceiro ela entrou; o aluno ja sabe que e dele e pergunta
 * ate quando vale e onde pode treinar — dai `unidades`, que o Response
 * operacional nao carrega.
 *
 * `temMatricula` falso e estado normal, nao erro: aluno recem-cadastrado
 * ou com a matricula cancelada tem uma resposta a receber, e ela e "voce
 * nao tem matricula vigente", nao um 404.
 *
 * Das unidades sai so o nome. O aluno precisa saber se o plano cobre a
 * unidade onde ele quer treinar; endereco e telefone da rede inteira
 * foram justamente o que se tirou dele quando GET /api/unidades deixou
 * de ser authenticated().
 */
public record MinhaMatriculaResponse(
        boolean temMatricula,
        String planoNome,
        BigDecimal valorMensal,
        TipoCobranca tipoCobranca,
        OrigemAssinatura origem,
        LocalDate dataInicio,
        LocalDate dataVencimento,
        StatusAssinatura status,
        /** Derivado da data pelo servidor, nao gravado. */
        boolean vencida,
        /** Negativo quando ja venceu. Contado aqui, nunca no navegador. */
        long diasParaVencer,
        /** Nomes das unidades que o plano cobre. */
        List<String> unidades
) {

    public static MinhaMatriculaResponse semMatricula() {
        return new MinhaMatriculaResponse(
                false, null, null, null, null, null, null, null, false, 0, List.of());
    }

    public static MinhaMatriculaResponse de(Assinatura assinatura, LocalDate hoje) {
        return new MinhaMatriculaResponse(
                true,
                assinatura.getPlano().getNome(),
                assinatura.getPlano().getValorMensal(),
                assinatura.getPlano().getTipoCobranca(),
                assinatura.getOrigem(),
                assinatura.getDataInicio(),
                assinatura.getDataVencimento(),
                assinatura.getStatus(),
                assinatura.estaVencidaEm(hoje),
                // O mesmo dia de hoje que decide `vencida` e o veredito da
                // catraca. Se o front contasse, um relogio adiantado faria a
                // tela do aluno discordar da catraca sobre ele mesmo.
                ChronoUnit.DAYS.between(hoje, assinatura.getDataVencimento()),
                assinatura.getPlano().getUnidades().stream()
                        .map(Unidade::getNome)
                        .toList()
        );
    }
}
