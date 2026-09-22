-- ==========================================================
-- Comissao de indicacao: a recompensa de quem trouxe um aluno novo.
--
-- Nasce PENDENTE no primeiro pagamento confirmado da assinatura
-- indicada -- matricula que cancela antes de pagar nada nao gera
-- recompensa. A secretaria revisa e aplica o desconto na proxima
-- cobranca pendente do indicador; nao e automatico de proposito, pelo
-- mesmo motivo de nao haver CHECK de coerencia aqui -- um erro de regra
-- nao pode sair descontando dinheiro sozinho.
--
-- UNIQUE em assinatura_id: no maximo uma comissao por indicacao, nunca
-- duplicada mesmo se a renovacao acabar processando a mesma assinatura
-- mais de uma vez.
-- ==========================================================

CREATE TABLE matriculas.comissoes_indicacao (
    id BIGSERIAL PRIMARY KEY,
    assinatura_id BIGINT NOT NULL REFERENCES matriculas.assinaturas_alunos(id),
    indicador_id BIGINT NOT NULL REFERENCES core.usuarios(id),
    valor DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, APLICADA
    cobranca_aplicada_id BIGINT REFERENCES matriculas.cobrancas(id),
    data_criacao TIMESTAMP NOT NULL DEFAULT now(),
    data_resolucao TIMESTAMP,
    UNIQUE (assinatura_id)
);

CREATE INDEX idx_comissao_indicacao_status ON matriculas.comissoes_indicacao (status);
