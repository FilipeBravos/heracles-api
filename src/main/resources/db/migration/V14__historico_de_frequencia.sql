-- ==========================================================
-- Historico de frequencia: cada check-in na catraca/recepcao.
--
-- Nasce do mesmo veredito que GET /assinaturas/acesso ja calculava —
-- so passa a ficar gravado. Grava liberado e barrado, de proposito:
-- uma tentativa barrada tambem e frequencia, e e o que explica pra
-- secretaria por que o aluno reclamou na porta.
-- ==========================================================

CREATE TABLE matriculas.checkins (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES core.usuarios(id),
    unidade_id BIGINT NOT NULL REFERENCES core.unidades(id),
    -- Nula quando o motivo e SEM_MATRICULA: nao ha assinatura pra apontar.
    assinatura_id BIGINT REFERENCES matriculas.assinaturas_alunos(id),
    momento TIMESTAMP NOT NULL DEFAULT now(),
    liberado BOOLEAN NOT NULL,
    motivo VARCHAR(30) NOT NULL
        CHECK (motivo IN ('LIBERADO', 'SEM_MATRICULA', 'INADIMPLENTE', 'VENCIDA', 'UNIDADE_NAO_COBERTA'))
);

-- O historico de um aluno e sempre lido do mais recente pro mais antigo.
CREATE INDEX idx_checkin_aluno_momento ON matriculas.checkins (aluno_id, momento DESC);
CREATE INDEX idx_checkin_unidade_momento ON matriculas.checkins (unidade_id, momento DESC);
