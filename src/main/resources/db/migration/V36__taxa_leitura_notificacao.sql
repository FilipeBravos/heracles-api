-- ==========================================================
-- Tempo ate a leitura de uma notificacao.
--
-- Ate aqui, `lida` so dizia se a notificacao foi lida, nunca quando —
-- nao dava pra saber se um aviso chega a ser visto a tempo. Nula nas
-- notificacoes ja lidas antes desta coluna existir: momento da leitura
-- desconhecido, nunca inferido a partir de outra data.
-- ==========================================================

ALTER TABLE core.notificacoes
    ADD COLUMN lida_em TIMESTAMP;
