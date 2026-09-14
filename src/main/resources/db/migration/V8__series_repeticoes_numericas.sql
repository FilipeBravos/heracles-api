-- ==========================================================
-- Prescricao do exercicio deixa de ser texto livre.
--
-- Ate aqui, "repeticoes" guardava a prescricao inteira numa string
-- ("4x10 a 12", "3x12"). Da para exibir, mas nao da para somar: nao existe
-- consulta que responda "qual o volume semanal desta ficha" sem reparsear
-- texto a cada leitura.
--
-- O intervalo vira min/max em vez de um unico inteiro porque os dados que
-- ja existem tem faixas ("10 a 12"). Com uma coluna so, a migracao teria
-- de escolher um dos extremos e descartar o outro — perda de informacao
-- numa migracao, que e justamente o que nao se pode fazer. Quando a
-- prescricao e exata, min e max ficam iguais.
--
-- "carga" e VARCHAR de proposito: core.treinos e um MODELO reutilizavel,
-- compartilhado entre alunos (N:N via core.usuario_treinos). A carga que
-- cabe aqui e a prescricao ("ate a falha", "70% 1RM", "peso corporal"),
-- nao um numero — o peso levantado varia por aluno e por sessao, e pertence
-- a um registro de execucao que ainda nao existe.
-- ==========================================================

ALTER TABLE core.exercicios
    ADD COLUMN series          INT,
    ADD COLUMN repeticoes_min  INT,
    ADD COLUMN repeticoes_max  INT,
    ADD COLUMN carga           VARCHAR(50);

-- ----------------------------------------------------------
-- Converte o texto existente. Formatos cobertos: "4x12", "4 x 12",
-- "4x10 a 12", "4x10-12".
-- ----------------------------------------------------------
UPDATE core.exercicios
SET series = NULLIF(substring(repeticoes from '^\s*(\d+)\s*[xX]'), '')::int,
    repeticoes_min = NULLIF(substring(repeticoes from '[xX]\s*(\d+)'), '')::int,
    repeticoes_max = COALESCE(
        -- limite superior de uma faixa ("10 a 12" / "10-12")
        NULLIF(substring(repeticoes from '[xX]\s*\d+\s*(?:[aA]|-|ate|até)\s*(\d+)'), '')::int,
        -- sem faixa: max = min
        NULLIF(substring(repeticoes from '[xX]\s*(\d+)'), '')::int
    )
WHERE repeticoes ~ '^\s*\d+\s*[xX]\s*\d+';

-- ----------------------------------------------------------
-- Linhas que nao seguem o padrao ("ate a falha", "AMRAP", texto livre).
-- Nada e descartado: o texto original vai para as observacoes, e a
-- prescricao numerica fica no minimo possivel para o professor corrigir.
-- ----------------------------------------------------------
UPDATE core.exercicios
SET observacoes = TRIM(BOTH ' ' FROM
        CASE
            WHEN observacoes IS NULL OR observacoes = '' THEN ''
            ELSE observacoes || ' | '
        END || 'Prescricao original: ' || repeticoes),
    series = 1,
    repeticoes_min = 1,
    repeticoes_max = 1
WHERE series IS NULL;

ALTER TABLE core.exercicios
    ALTER COLUMN series         SET NOT NULL,
    ALTER COLUMN repeticoes_min SET NOT NULL,
    ALTER COLUMN repeticoes_max SET NOT NULL;

ALTER TABLE core.exercicios DROP COLUMN repeticoes;

-- ----------------------------------------------------------
-- O banco passa a recusar prescricao impossivel, independente de quem
-- escreve — a validacao da aplicacao deixa de ser a unica barreira.
-- ----------------------------------------------------------
ALTER TABLE core.exercicios
    ADD CONSTRAINT ck_exercicio_series CHECK (series BETWEEN 1 AND 20),
    ADD CONSTRAINT ck_exercicio_repeticoes CHECK (
        repeticoes_min BETWEEN 1 AND 500
        AND repeticoes_max BETWEEN 1 AND 500
        AND repeticoes_max >= repeticoes_min
    );
