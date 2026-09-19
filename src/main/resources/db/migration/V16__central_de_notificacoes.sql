-- ==========================================================
-- Central de notificacoes.
--
-- Sem SMTP nem servico de push configurado no projeto: o aviso vive
-- dentro do proprio app, gerado por jobs diarios e lido no sino da
-- barra superior. E-mail/push de verdade fica pronto para plugar por
-- cima disto quando (e se) o projeto ganhar essa infraestrutura.
-- ==========================================================

CREATE TABLE core.notificacoes (
    id BIGSERIAL PRIMARY KEY,
    destinatario_id BIGINT NOT NULL REFERENCES core.usuarios(id),
    tipo VARCHAR(30) NOT NULL
        CHECK (tipo IN ('MATRICULA_VENCENDO', 'ANAMNESE_PENDENTE', 'ANIVERSARIO')),
    titulo VARCHAR(100) NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    -- Aponta pro que originou o aviso (assinatura, aluno...), sem FK de
    -- verdade porque o alvo muda conforme o tipo. Nulo no resumo diario
    -- de anamnese pendente, que nao e sobre um aluno so.
    referencia_id BIGINT,
    criada_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notificacao_destinatario ON core.notificacoes (destinatario_id, criada_em DESC);

-- A contagem de nao lidas (o numero no sino) e a consulta mais frequente.
CREATE INDEX idx_notificacao_nao_lida ON core.notificacoes (destinatario_id) WHERE NOT lida;
