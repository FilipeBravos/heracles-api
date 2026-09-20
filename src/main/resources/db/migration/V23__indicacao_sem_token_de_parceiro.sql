-- ==========================================================
-- ck_assinatura_token_parceiro (da V10) so previa DIRETO (sem token) e
-- GYMPASS/TOTALPASS (com token) — INDICACAO nao caia em nenhum dos dois
-- ramos e violava o CHECK. Indicacao e como matricula direta quanto a
-- token: nao tem, porque quem identifica o aluno e o indicador, nao um
-- parceiro externo.
-- ==========================================================

ALTER TABLE matriculas.assinaturas_alunos
    DROP CONSTRAINT ck_assinatura_token_parceiro,
    ADD CONSTRAINT ck_assinatura_token_parceiro CHECK (
        (origem IN ('DIRETO', 'INDICACAO') AND token_parceiro IS NULL)
     OR (origem IN ('GYMPASS', 'TOTALPASS') AND token_parceiro IS NOT NULL)
    );
