package br.com.heracles.heracles_api.matriculas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Renovacao automatica de quem paga no cartao — roda antes do bloqueio
 * automatico (6h) de proposito: um cartao renovado as 5h45 nunca chega
 * a ser marcado vencido nem inadimplente no mesmo dia.
 */
@Component
public class RenovacaoAutomaticaScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenovacaoAutomaticaScheduler.class);

    private final AssinaturaService service;

    public RenovacaoAutomaticaScheduler(AssinaturaService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 45 5 * * *")
    public void renovarAutomaticamente() {
        int quantidade = service.renovarAutomaticamente();
        if (quantidade > 0) {
            log.info("Renovacao automatica: {} assinatura(s) no cartao cobrada(s) e renovada(s).", quantidade);
        }
    }
}
