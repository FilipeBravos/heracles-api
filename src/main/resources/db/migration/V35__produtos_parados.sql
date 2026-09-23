-- ==========================================================
-- Produtos parados: o oposto do estoque baixo.
--
-- Ate aqui nao havia nenhuma nocao de "cadastro" do produto — sem essa
-- data, um produto recem-cadastrado e sem venda ainda ficaria
-- indistinguivel de um produto que nunca vende. DEFAULT now() e uma
-- aproximacao deliberada pro que ja existe: ninguem tinha essa data
-- registrada antes, e tratar todo produto antigo como "cadastrado
-- hoje" e mais seguro do que presumir que ja esta parado desde sempre.
-- ==========================================================

ALTER TABLE operacoes.produtos_suplementos
    ADD COLUMN cadastrado_em TIMESTAMP NOT NULL DEFAULT now();
