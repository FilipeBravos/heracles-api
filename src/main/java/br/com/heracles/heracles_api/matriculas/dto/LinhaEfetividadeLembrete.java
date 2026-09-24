package br.com.heracles.heracles_api.matriculas.dto;

import br.com.heracles.heracles_api.matriculas.domain.CanalLembrete;
import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;

import java.math.BigDecimal;

/**
 * Efetividade de um estagio/canal de lembrete: quantos foram enviados no
 * periodo e quantos converteram em pagamento, do pior pro melhor.
 *
 * `diasMediosParaConversao` e nulo quando nenhum lembrete deste grupo
 * converteu — nao ha media para tirar de zero conversoes.
 */
public record LinhaEfetividadeLembrete(
        EstagioLembrete estagio, CanalLembrete canal,
        long totalEnviados, long totalConvertidos,
        BigDecimal taxaConversao, BigDecimal diasMediosParaConversao) {
}
