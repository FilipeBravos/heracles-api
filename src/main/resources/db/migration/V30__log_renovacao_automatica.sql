-- ==========================================================
-- Log de execucoes da renovacao automatica no cartao.
--
-- So historico, sem alerta ativo: o job roda todo dia sozinho, sem
-- gateway de pagamento de verdade, e ate agora nao deixava rastro
-- nenhum de quando rodou ou quantas assinaturas renovou. Um dia sem
-- registro aqui ja denuncia sozinho que o job nao rodou ou falhou.
-- ==========================================================

CREATE TABLE matriculas.execucoes_renovacao_automatica (
    id BIGSERIAL PRIMARY KEY,
    data_execucao TIMESTAMP NOT NULL DEFAULT now(),
    quantidade_renovada INT NOT NULL
);
