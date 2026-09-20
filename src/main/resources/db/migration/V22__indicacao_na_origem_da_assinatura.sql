-- ==========================================================
-- A V21 criou a coluna de indicacao, mas esqueceu que origem tem um
-- CHECK proprio (ck_assinatura_origem, da V10) que so aceitava DIRETO,
-- GYMPASS e TOTALPASS — sem este ajuste, toda matricula por indicacao
-- cai com erro de banco em vez do 400/409 legivel do service.
-- ==========================================================

ALTER TABLE matriculas.assinaturas_alunos
    DROP CONSTRAINT ck_assinatura_origem,
    ADD CONSTRAINT ck_assinatura_origem
        CHECK (origem IN ('DIRETO', 'GYMPASS', 'TOTALPASS', 'INDICACAO'));
