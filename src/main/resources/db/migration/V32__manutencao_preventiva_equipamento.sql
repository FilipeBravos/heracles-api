-- ==========================================================
-- Manutencao preventiva de equipamentos.
--
-- Ate aqui so existia manutencao corretiva (historico_manutencao,
-- aberta quando o aparelho ja quebrou) e nenhuma nocao de "esta
-- vencendo". Cada equipamento pode ganhar um intervalo (em dias) pra
-- ser revisado antes disso — nulo significa sem acompanhamento
-- preventivo configurado, o que preserva o comportamento atual de
-- todo equipamento ja cadastrado.
--
-- cadastrado_em ancora a contagem quando o equipamento nunca teve
-- nenhum chamado resolvido ainda. DEFAULT now() e uma aproximacao
-- deliberada pro que ja existe: ninguem tinha essa data registrada
-- antes, e comecar a contagem hoje e mais seguro do que presumir que
-- um equipamento antigo ja esta vencido assim que alguem configurar
-- o intervalo.
-- ==========================================================

ALTER TABLE operacoes.equipamentos
    ADD COLUMN intervalo_dias_manutencao INT,
    ADD COLUMN cadastrado_em TIMESTAMP NOT NULL DEFAULT now();
