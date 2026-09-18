package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;

import java.time.LocalDate;

/**
 * Uma ficha que o aluno já treinou e não treina mais.
 *
 * Nome, foco e nivel vem do registro, nao da ficha viva: ela pode ter
 * sido renomeada ou apagada desde entao, e o historico descreve o que o
 * aluno treinou naquele periodo, nao o que a ficha e hoje.
 */
public record HistoricoTreinoResponse(
        String nome,
        String foco,
        String nivel,
        LocalDate vinculadoEm,
        LocalDate desvinculadoEm
) {
    public static HistoricoTreinoResponse de(HistoricoTreinoAluno historico) {
        return new HistoricoTreinoResponse(
                historico.getTreinoNome(),
                historico.getTreinoFoco(),
                historico.getTreinoNivel(),
                historico.getVinculadoEm().toLocalDate(),
                historico.getDesvinculadoEm().toLocalDate()
        );
    }
}
