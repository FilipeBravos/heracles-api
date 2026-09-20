package br.com.heracles.heracles_api.matriculas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lembretes automaticos de vencimento/inadimplencia — um por estagio da
 * regua, nunca repetido.
 *
 * Roda entre o bloqueio automatico (6h) e a central de notificacoes
 * (6h30): nao ha dependencia real entre os tres, so espacamento de bom
 * senso para nao competir por conexao de banco no mesmo minuto.
 */
@Component
public class LembreteScheduler {

    private static final Logger log = LoggerFactory.getLogger(LembreteScheduler.class);

    private final AssinaturaService service;

    public LembreteScheduler(AssinaturaService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 15 6 * * *")
    public void gerarLembretesDiarios() {
        int quantidade = service.gerarLembretes(AssinaturaService.DIAS_VENCE_EM_BREVE_PADRAO);
        if (quantidade > 0) {
            log.info("Lembretes automaticos: {} novo(s) (vence em breve, vencida ou inadimplente).", quantidade);
        }
    }
}
