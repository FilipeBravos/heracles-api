-- ==========================================================
-- Confirmacao de presenca em aula em grupo: o professor confirma quem
-- compareceu depois que a aula aconteceu, do jeito que ja confirma a
-- realizacao de uma sessao de personal. Nula ate ser confirmada.
-- ==========================================================

ALTER TABLE agenda.inscricoes_aula
    ADD COLUMN presente boolean,
    ADD COLUMN presenca_confirmada_em timestamp;
