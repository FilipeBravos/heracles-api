package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.CanalLembrete;
import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;
import br.com.heracles.heracles_api.matriculas.domain.LembreteEnviado;

import java.time.LocalDateTime;

public final class LembreteDtos {

    private LembreteDtos() {
    }

    public record Response(
            Long id,
            EstagioLembrete estagio,
            CanalLembrete canal,
            String destinatario,
            LocalDateTime dataEnvio
    ) {
        public static Response de(LembreteEnviado lembrete) {
            return new Response(
                    lembrete.getId(),
                    lembrete.getEstagio(),
                    lembrete.getCanal(),
                    lembrete.getDestinatario(),
                    lembrete.getDataEnvio()
            );
        }
    }
}
