-- ==========================================================
-- Lembretes automaticos de vencimento/inadimplencia.
--
-- Simulado de proposito, no mesmo espirito de matriculas.cobrancas: nao
-- ha WhatsApp nem SMTP integrado no projeto, entao "enviar" aqui e so
-- gravar o que teria saido, sem nenhuma chamada externa de verdade.
--
-- No maximo um lembrete por (assinatura, estagio) — virar de estagio
-- (vence em breve -> vencida -> inadimplente) e o que libera um novo.
-- ==========================================================

CREATE TABLE matriculas.lembretes_enviados (
    id BIGSERIAL PRIMARY KEY,
    assinatura_id BIGINT NOT NULL REFERENCES matriculas.assinaturas_alunos(id) ON DELETE CASCADE,
    estagio VARCHAR(20) NOT NULL, -- VENCE_EM_BREVE, VENCIDA, INADIMPLENTE
    canal VARCHAR(20) NOT NULL, -- WHATSAPP, EMAIL
    destinatario VARCHAR(150) NOT NULL,
    data_envio TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (assinatura_id, estagio)
);

CREATE INDEX idx_lembrete_assinatura ON matriculas.lembretes_enviados (assinatura_id);
