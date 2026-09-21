-- ==========================================================
-- V25 criou nota_avaliacao como SMALLINT, mas o campo Java e
-- Integer (mapeia para INTEGER por padrao) — Hibernate recusa a
-- subir com esse descompasso de tipo. SMALLINT -> INTEGER e um
-- alargamento direto, sem risco: nenhuma linha ainda usa a coluna.
-- ==========================================================

ALTER TABLE agenda.sessoes_personal
    ALTER COLUMN nota_avaliacao TYPE INTEGER;
