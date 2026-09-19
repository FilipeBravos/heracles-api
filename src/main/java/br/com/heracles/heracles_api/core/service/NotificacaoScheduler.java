package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.matriculas.service.AssinaturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O job diario da central de notificacoes.
 *
 * Roda depois do job da regua de cobranca (6h) para nao competir por
 * conexao de banco no mesmo minuto — nao ha dependencia real entre os
 * dois, so um espacamento de bom senso.
 */
@Component
public class NotificacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoScheduler.class);

    private final NotificacaoService service;

    public NotificacaoScheduler(NotificacaoService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 30 6 * * *")
    public void gerarNotificacoesDiarias() {
        int matricula = service.gerarNotificacoesMatriculaVencendo(AssinaturaService.DIAS_VENCE_EM_BREVE_PADRAO);
        int anamnese = service.gerarNotificacoesAnamnesePendente();
        int aniversario = service.gerarNotificacoesAniversario();

        int total = matricula + anamnese + aniversario;
        if (total > 0) {
            log.info("Central de notificacoes: {} nova(s) (matricula: {}, anamnese: {}, aniversario: {}).",
                    total, matricula, anamnese, aniversario);
        }
    }
}
