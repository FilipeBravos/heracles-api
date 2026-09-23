-- ==========================================================
-- Estoque minimo por produto, para a sugestao de reposicao.
--
-- Antes so existia um limiar global (5) hardcoded no dashboard, o mesmo
-- pra qualquer produto — um suplemento que vende uma unidade por mes e
-- uma toalha que vende vinte por semana tinham o mesmo aviso de "acabando".
-- DEFAULT 5 preserva o comportamento atual pra todo produto ja cadastrado;
-- a partir daqui cada um pode ajustar o proprio limiar.
-- ==========================================================

ALTER TABLE operacoes.produtos_suplementos
    ADD COLUMN estoque_minimo INT NOT NULL DEFAULT 5;
