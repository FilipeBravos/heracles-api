-- ==========================================================
-- Sessao de personal ganha o status REALIZADA: quem confirma que
-- a sessao aconteceu e o professor (foi ele quem esteve la), nao
-- a secretaria que so agendou. So depois de REALIZADA o aluno
-- pode avaliar a sessao — nota de 1 a 5, comentario opcional.
-- ==========================================================

ALTER TABLE agenda.sessoes_personal
    DROP CONSTRAINT sessoes_personal_status_check;

ALTER TABLE agenda.sessoes_personal
    ADD CONSTRAINT sessoes_personal_status_check
        CHECK (status IN ('AGENDADO', 'REALIZADA', 'CANCELADO'));

ALTER TABLE agenda.sessoes_personal
    ADD COLUMN nota_avaliacao SMALLINT,
    ADD COLUMN comentario_avaliacao VARCHAR(500);

ALTER TABLE agenda.sessoes_personal
    ADD CONSTRAINT ck_sessao_personal_nota CHECK (
        nota_avaliacao IS NULL OR nota_avaliacao BETWEEN 1 AND 5
    ),
    ADD CONSTRAINT ck_sessao_personal_nota_realizada CHECK (
        nota_avaliacao IS NULL OR status = 'REALIZADA'
    );
