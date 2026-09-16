package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.TipoCobranca;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class PlanoDtos {

    private PlanoDtos() {
    }

    public record Request(
            @NotBlank(message = "O nome do plano e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome,

            @NotNull(message = "Informe o valor mensal")
            @DecimalMin(value = "0.01", message = "O valor mensal precisa ser maior que zero")
            @Digits(integer = 8, fraction = 2, message = "Valor invalido")
            BigDecimal valorMensal,

            @NotNull(message = "Informe o tipo de cobranca")
            TipoCobranca tipoCobranca,

            /**
             * Um plano sem unidade nao da acesso a lugar nenhum — seria um
             * plano que o aluno paga e nao usa. A regra vive tambem no
             * servico, porque nenhuma constraint de tabela alcanca isto.
             */
            @NotEmpty(message = "Selecione ao menos uma unidade")
            Set<Long> unidadeIds
    ) {
    }

    public record UnidadeResumo(Long id, String nome) {
    }

    public record Response(
            Long id,
            String nome,
            BigDecimal valorMensal,
            TipoCobranca tipoCobranca,
            boolean ativo,
            List<UnidadeResumo> unidades
    ) {
        public static Response de(Plano plano) {
            return new Response(
                    plano.getId(),
                    plano.getNome(),
                    plano.getValorMensal(),
                    plano.getTipoCobranca(),
                    plano.isAtivo(),
                    plano.getUnidades().stream()
                            .map(unidade -> new UnidadeResumo(unidade.getId(), unidade.getNome()))
                            .toList()
            );
        }
    }
}
