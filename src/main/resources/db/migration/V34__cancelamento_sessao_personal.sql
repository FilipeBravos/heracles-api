-- ==========================================================
-- Antecedencia do cancelamento de sessao de personal.
--
-- Ate aqui, cancelar so trocava o status pra CANCELADO — nada dizia
-- quando o cancelamento aconteceu, entao nao dava pra distinguir um
-- cancelamento com uma semana de antecedencia de um em cima da hora,
-- que e o que de fato atrapalha a agenda do personal. Nulo nas linhas
-- ja canceladas antes desta coluna existir: antecedencia desconhecida,
-- nao "cancelado em cima da hora".
-- ==========================================================

ALTER TABLE agenda.sessoes_personal
    ADD COLUMN cancelado_em TIMESTAMP;
