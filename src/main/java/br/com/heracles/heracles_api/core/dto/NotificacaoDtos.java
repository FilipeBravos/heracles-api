package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Notificacao;
import br.com.heracles.heracles_api.core.domain.TipoNotificacao;

import java.time.LocalDateTime;

public final class NotificacaoDtos {

    private NotificacaoDtos() {
    }

    public record Response(
            Long id,
            TipoNotificacao tipo,
            String titulo,
            String mensagem,
            boolean lida,
            LocalDateTime criadaEm
    ) {
        public static Response de(Notificacao notificacao) {
            return new Response(
                    notificacao.getId(),
                    notificacao.getTipo(),
                    notificacao.getTitulo(),
                    notificacao.getMensagem(),
                    notificacao.isLida(),
                    notificacao.getCriadaEm()
            );
        }
    }

    /** So a contagem de nao lidas — e o numero que o sino mostra sem abrir a lista. */
    public record Resumo(long naoLidas) {
    }
}
