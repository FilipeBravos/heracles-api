-- ==========================================================
-- Avaliacao fisica periodica: peso, medidas e foto de evolucao.
--
-- Complementar a anamnese (intake unico, atualizado no lugar) — aqui
-- cada visita gera uma linha nova, e a evolucao esta em comparar uma
-- avaliacao com a anterior. Sem UPDATE de proposito: uma medida errada
-- se corrige com uma avaliacao nova, nao reescrevendo o passado.
-- ==========================================================

CREATE TABLE core.avaliacoes_fisicas (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES core.usuarios(id),
    data DATE NOT NULL,
    peso_kg NUMERIC(5,2) NOT NULL CHECK (peso_kg > 0),
    altura_cm NUMERIC(5,1) NOT NULL CHECK (altura_cm > 0),
    percentual_gordura NUMERIC(4,1) CHECK (percentual_gordura IS NULL OR percentual_gordura BETWEEN 0 AND 100),
    circunferencia_cintura NUMERIC(5,1),
    circunferencia_quadril NUMERIC(5,1),
    circunferencia_braco NUMERIC(5,1),
    circunferencia_coxa NUMERIC(5,1),
    observacoes TEXT,
    -- Mesmo padrao da foto do aluno (core.usuarios.foto): bytes no
    -- proprio banco, sem infraestrutura de upload multipart.
    foto BYTEA,
    foto_content_type VARCHAR(50),
    data_criacao TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_avaliacao_fisica_aluno_data ON core.avaliacoes_fisicas (aluno_id, data DESC);
