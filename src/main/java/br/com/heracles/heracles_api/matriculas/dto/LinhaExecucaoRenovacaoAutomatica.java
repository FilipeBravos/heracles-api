package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.ExecucaoRenovacaoAutomatica;

import java.time.LocalDateTime;

/** Uma linha do log de renovacao automatica: quando o job rodou e quantas assinaturas renovou. */
public record LinhaExecucaoRenovacaoAutomatica(Long id, LocalDateTime dataExecucao, int quantidadeRenovada) {
    public static LinhaExecucaoRenovacaoAutomatica de(ExecucaoRenovacaoAutomatica execucao) {
        return new LinhaExecucaoRenovacaoAutomatica(
                execucao.getId(), execucao.getDataExecucao(), execucao.getQuantidadeRenovada());
    }
}
