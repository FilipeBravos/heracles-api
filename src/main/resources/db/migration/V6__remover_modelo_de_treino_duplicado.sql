-- ==========================================================
-- Consolidacao do modelo de treinos.
--
-- A V1 criou treinos.fichas_treino / treinos.exercicios_ficha para
-- representar a ficha de UM aluno (aluno_id + professor_id + validade).
-- A V2/V3 criaram core.treinos / core.exercicios representando um
-- MODELO de treino reutilizavel, e a V5 ligou aluno<->treino por uma
-- tabela de juncao N:N.
--
-- Sao dois modelos de dominio diferentes para a mesma tela, e apenas o
-- segundo tem entidade, repositorio, endpoint e dados reais. As tabelas
-- da V1 nunca receberam uma linha. Removemos o ramo morto para que exista
-- uma unica resposta para "onde mora um treino".
-- ==========================================================

DROP TABLE IF EXISTS treinos.exercicios_ficha;
DROP TABLE IF EXISTS treinos.fichas_treino;

-- ==========================================================
-- Ordem dos exercicios dentro da ficha.
--
-- A lista era devolvida sem ORDER BY, entao a ordem dependia do plano de
-- execucao do Postgres. Numa ficha de academia a sequencia dos exercicios
-- e parte da prescricao, nao um detalhe de apresentacao.
-- ==========================================================

ALTER TABLE core.exercicios ADD COLUMN IF NOT EXISTS ordem INT;

-- Congela a ordem atual (por id) para as fichas que ja existem.
UPDATE core.exercicios e
SET ordem = sub.posicao
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY treino_id ORDER BY id) - 1 AS posicao
    FROM core.exercicios
) sub
WHERE e.id = sub.id AND e.ordem IS NULL;

ALTER TABLE core.exercicios ALTER COLUMN ordem SET NOT NULL;

-- ==========================================================
-- Indices de apoio as consultas que a aplicacao realmente faz.
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_exercicios_treino ON core.exercicios (treino_id, ordem);
CREATE INDEX IF NOT EXISTS idx_usuario_treinos_treino ON core.usuario_treinos (treino_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_status ON core.usuarios (status);
