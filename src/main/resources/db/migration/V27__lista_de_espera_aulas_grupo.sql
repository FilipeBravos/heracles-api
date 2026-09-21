-- ==========================================================
-- Lista de espera em aulas em grupo: quando a turma esta lotada, a
-- inscricao vira EM_ESPERA em vez de ser recusada. Quando uma vaga se
-- abre (alguem com INSCRITA cancela), quem espera ha mais tempo entra
-- automaticamente e e avisado pela central de notificacoes.
-- ==========================================================

ALTER TABLE agenda.inscricoes_aula
    DROP CONSTRAINT inscricoes_aula_status_check,
    ADD CONSTRAINT inscricoes_aula_status_check
        CHECK (status IN ('INSCRITA', 'EM_ESPERA', 'CANCELADA'));

ALTER TABLE core.notificacoes
    DROP CONSTRAINT notificacoes_tipo_check,
    ADD CONSTRAINT notificacoes_tipo_check
        CHECK (tipo IN ('MATRICULA_VENCENDO', 'ANAMNESE_PENDENTE', 'ANIVERSARIO', 'VAGA_LIBERADA'));
