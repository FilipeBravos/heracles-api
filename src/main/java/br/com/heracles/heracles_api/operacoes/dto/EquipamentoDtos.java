package br.com.heracles.heracles_api.operacoes.dto;

import br.com.heracles.heracles_api.operacoes.domain.ChamadoManutencao;
import br.com.heracles.heracles_api.operacoes.domain.Equipamento;
import br.com.heracles.heracles_api.operacoes.domain.StatusChamado;
import br.com.heracles.heracles_api.operacoes.domain.StatusEquipamento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EquipamentoDtos {

    private EquipamentoDtos() {
    }

    public record Request(
            @NotNull(message = "Informe a unidade do equipamento")
            Long unidadeId,

            @NotBlank(message = "O nome do equipamento e obrigatorio")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String nome
    ) {
        // O status nao entra aqui de proposito: ele e consequencia dos
        // chamados de manutencao, nao um campo editavel a mao.
    }

    public record AbrirChamado(
            @NotBlank(message = "Descreva o problema")
            @Size(max = 2000, message = "Descricao longa demais")
            String descricaoProblema
    ) {
    }

    public record ResolverChamado(
            @DecimalMin(value = "0.00", message = "O custo nao pode ser negativo")
            @Digits(integer = 8, fraction = 2, message = "Custo invalido")
            BigDecimal custoReparo
    ) {
    }

    public record Response(
            Long id,
            Long unidadeId,
            String unidadeNome,
            String nome,
            StatusEquipamento statusAtual
    ) {
        public static Response de(Equipamento equipamento) {
            return new Response(
                    equipamento.getId(),
                    equipamento.getUnidade().getId(),
                    equipamento.getUnidade().getNome(),
                    equipamento.getNome(),
                    equipamento.getStatusAtual()
            );
        }
    }

    public record ChamadoResponse(
            Long id,
            LocalDateTime dataChamado,
            String descricaoProblema,
            BigDecimal custoReparo,
            StatusChamado status,
            LocalDateTime dataResolucao
    ) {
        public static ChamadoResponse de(ChamadoManutencao chamado) {
            return new ChamadoResponse(
                    chamado.getId(),
                    chamado.getDataChamado(),
                    chamado.getDescricaoProblema(),
                    chamado.getCustoReparo(),
                    chamado.getStatus(),
                    chamado.getDataResolucao()
            );
        }
    }
}
