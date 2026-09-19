package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.Checkin;
import br.com.heracles.heracles_api.matriculas.domain.MotivoAcesso;

import java.time.LocalDateTime;

public final class CheckinDtos {

    private CheckinDtos() {
    }

    /** Uma linha do historico de frequencia do aluno. */
    public record Response(
            Long id,
            String unidadeNome,
            LocalDateTime momento,
            boolean liberado,
            MotivoAcesso motivo
    ) {
        public static Response de(Checkin checkin) {
            return new Response(
                    checkin.getId(),
                    checkin.getUnidade().getNome(),
                    checkin.getMomento(),
                    checkin.isLiberado(),
                    checkin.getMotivo()
            );
        }
    }
}
