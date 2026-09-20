-- ==========================================================
-- Avaliacao fisica: peito entra nas circunferencias, e a foto vira
-- galeria (frente, lado, costas) em vez de uma unica coluna.
--
-- Sem UPDATE aqui tambem: a galeria e populada so na criacao da
-- avaliacao, no mesmo espirito de "avaliacao errada vira uma nova, nao
-- uma reescrita".
-- ==========================================================

ALTER TABLE core.avaliacoes_fisicas
    ADD COLUMN circunferencia_peito NUMERIC(5,1);

CREATE TABLE core.avaliacoes_fisicas_fotos (
    id BIGSERIAL PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL REFERENCES core.avaliacoes_fisicas(id) ON DELETE CASCADE,
    -- Mesmo padrao da foto do aluno (core.usuarios.foto): bytes no
    -- proprio banco, sem infraestrutura de upload multipart.
    foto BYTEA NOT NULL,
    foto_content_type VARCHAR(50) NOT NULL,
    ordem INT NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_avaliacao_fisica_foto_avaliacao ON core.avaliacoes_fisicas_fotos (avaliacao_id, ordem);

-- Leva a foto unica que ja existisse para a galeria antes de remover as
-- colunas antigas, para nao perder o que ja foi registrado.
INSERT INTO core.avaliacoes_fisicas_fotos (avaliacao_id, foto, foto_content_type, ordem, data_criacao)
SELECT id, foto, foto_content_type, 0, data_criacao
FROM core.avaliacoes_fisicas
WHERE foto IS NOT NULL;

ALTER TABLE core.avaliacoes_fisicas
    DROP COLUMN foto,
    DROP COLUMN foto_content_type;
