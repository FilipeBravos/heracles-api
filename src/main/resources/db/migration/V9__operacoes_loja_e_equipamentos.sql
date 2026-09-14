-- ==========================================================
-- Loja (PDV) e inventario de equipamentos.
--
-- As tabelas existem desde a V1, mas nunca tiveram entidade, endpoint
-- nem tela: foram criadas como esboco do produto e ficaram vazias. Esta
-- migracao as prepara para uso real — a V1 nao declarou uma unica
-- constraint alem das chaves estrangeiras.
--
-- Como nenhuma delas tem linha, os ALTER ... SET NOT NULL sao seguros.
-- ==========================================================

-- ==========================================================
-- Produtos
-- ==========================================================

-- Produto nao se apaga: ele sai de linha. Apagar quebraria o historico
-- de vendas (itens_venda referencia produto sem ON DELETE), e o preco
-- de uma venda passada precisa continuar rastreavel ao item vendido.
ALTER TABLE operacoes.produtos_suplementos
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE operacoes.produtos_suplementos
    ALTER COLUMN unidade_id SET NOT NULL,
    ADD CONSTRAINT ck_produto_estoque CHECK (quantidade_estoque >= 0),
    ADD CONSTRAINT ck_produto_preco CHECK (preco_venda > 0);

-- Mesmo produto duas vezes na mesma unidade e erro de cadastro.
CREATE UNIQUE INDEX IF NOT EXISTS uq_produto_unidade_nome
    ON operacoes.produtos_suplementos (unidade_id, lower(nome));

-- ==========================================================
-- Vendas
-- ==========================================================

ALTER TABLE operacoes.vendas_pdv
    ALTER COLUMN unidade_id SET NOT NULL,
    -- Quem operou o caixa fica registrado: sem isso nao ha a quem
    -- perguntar quando a venda nao bate com o estoque.
    ALTER COLUMN secretaria_id SET NOT NULL,
    ADD CONSTRAINT ck_venda_valor CHECK (valor_total >= 0),
    ADD CONSTRAINT ck_venda_pagamento
        CHECK (metodo_pagamento IN ('PIX', 'CREDITO', 'DEBITO', 'DINHEIRO'));

ALTER TABLE operacoes.itens_venda
    ALTER COLUMN venda_id SET NOT NULL,
    ALTER COLUMN produto_id SET NOT NULL,
    ADD CONSTRAINT ck_item_quantidade CHECK (quantidade > 0),
    ADD CONSTRAINT ck_item_preco CHECK (preco_unitario >= 0);

-- Um produto aparece uma vez por venda; quantidade repetida e somada
-- antes de gravar.
CREATE UNIQUE INDEX IF NOT EXISTS uq_item_venda_produto
    ON operacoes.itens_venda (venda_id, produto_id);

-- ==========================================================
-- Equipamentos
-- ==========================================================

ALTER TABLE operacoes.equipamentos
    ALTER COLUMN unidade_id SET NOT NULL,
    ALTER COLUMN status_atual SET DEFAULT 'OK',
    ADD CONSTRAINT ck_equipamento_status
        CHECK (status_atual IN ('OK', 'EM_MANUTENCAO'));

ALTER TABLE operacoes.historico_manutencao
    ADD COLUMN IF NOT EXISTS data_resolucao TIMESTAMP;

ALTER TABLE operacoes.historico_manutencao
    ALTER COLUMN equipamento_id SET NOT NULL,
    ADD CONSTRAINT ck_chamado_status CHECK (status IN ('ABERTO', 'RESOLVIDO')),
    ADD CONSTRAINT ck_chamado_custo CHECK (custo_reparo IS NULL OR custo_reparo >= 0),
    -- Custo e data de resolucao so existem depois de resolvido, e um
    -- chamado resolvido precisa dizer quando foi.
    ADD CONSTRAINT ck_chamado_resolucao CHECK (
        (status = 'ABERTO'    AND data_resolucao IS NULL AND custo_reparo IS NULL)
     OR (status = 'RESOLVIDO' AND data_resolucao IS NOT NULL)
    );

-- Um equipamento nao pode ter dois chamados abertos ao mesmo tempo.
-- Indice parcial: a restricao vale so entre os abertos, e o historico
-- de resolvidos cresce sem limite.
CREATE UNIQUE INDEX IF NOT EXISTS uq_chamado_aberto_por_equipamento
    ON operacoes.historico_manutencao (equipamento_id)
    WHERE status = 'ABERTO';

-- ==========================================================
-- Indices de apoio as consultas que as telas fazem
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_produto_unidade ON operacoes.produtos_suplementos (unidade_id);
CREATE INDEX IF NOT EXISTS idx_venda_unidade_data ON operacoes.vendas_pdv (unidade_id, data_venda DESC);
CREATE INDEX IF NOT EXISTS idx_item_venda ON operacoes.itens_venda (venda_id);
CREATE INDEX IF NOT EXISTS idx_equipamento_unidade ON operacoes.equipamentos (unidade_id, status_atual);
CREATE INDEX IF NOT EXISTS idx_chamado_equipamento ON operacoes.historico_manutencao (equipamento_id, data_chamado DESC);
