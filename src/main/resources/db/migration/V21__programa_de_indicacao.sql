-- ==========================================================
-- Programa de indicacao: qual aluno trouxe qual matricula nova.
--
-- So preenchido quando origem = INDICACAO (nao ha CHECK aqui pelo mesmo
-- motivo de token_parceiro: a coerencia mora no service, que devolve
-- 400/409 legivel em vez de a violacao de constraint virar 500).
-- ==========================================================

ALTER TABLE matriculas.assinaturas_alunos
    ADD COLUMN indicado_por_aluno_id BIGINT REFERENCES core.usuarios(id);

CREATE INDEX idx_assinatura_indicado_por ON matriculas.assinaturas_alunos (indicado_por_aluno_id);
