package br.com.heracles.heracles_api.matriculas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quando a regua de cobranca vira bloqueio automatico, sem depender de
 * alguem lembrar de clicar em "marcar inadimplente".
 *
 * Fica separado de AssinaturaService de proposito: a regra de negocio
 * (quem e "vencido demais") mora no servico e e testavel sem relogio nem
 * cron; esta classe so decide quando ela roda.
 */
@Component
public class InadimplenciaScheduler {

    private static final Logger log = LoggerFactory.getLogger(InadimplenciaScheduler.class);

    private final AssinaturaService service;
    private final int diasTolerancia;

    public InadimplenciaScheduler(
            AssinaturaService service,
            @Value("${heracles.inadimplencia.dias-tolerancia}") int diasTolerancia) {
        this.service = service;
        this.diasTolerancia = diasTolerancia;
    }

    /** Uma vez por dia, de madrugada — antes do expediente da secretaria comecar. */
    @Scheduled(cron = "0 0 6 * * *")
    public void autoBloquearVencidas() {
        int quantidade = service.autoBloquearVencidas(diasTolerancia);
        if (quantidade > 0) {
            log.info("Regua de cobranca: {} assinatura(s) marcada(s) como inadimplente (tolerancia de {} dias).",
                    quantidade, diasTolerancia);
        }
    }
}
