package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.ContratoAssinado;

import java.time.LocalDateTime;

public final class ContratoDtos {

    private ContratoDtos() {
    }

    /**
     * `assinado` sai sempre true aqui: só existe Response quando há um
     * ContratoAssinado salvo. Quem não tem (cadastro anterior a esta
     * funcionalidade) recebe Ausente, não um Response com campos nulos —
     * mesma convenção de AnamneseDtos.Response.
     */
    public record Response(
            boolean assinado,
            String nomeDigitado,
            String textoContrato,
            LocalDateTime assinadoEm
    ) {
        public static Response de(ContratoAssinado contrato) {
            return new Response(
                    true,
                    contrato.getNomeDigitado(),
                    contrato.getTextoContrato(),
                    contrato.getAssinadoEm()
            );
        }

        public static Response ausente() {
            return new Response(false, null, null, null);
        }
    }
}
