-- ==========================================================
-- Matriculas: planos e assinaturas de aluno.
--
-- As tres tabelas existem desde a V1 (planos, plano_unidades,
-- assinaturas_alunos) e nunca tiveram entidade, endpoint nem tela —
-- foram esboco do produto e ficaram vazias. Esta migracao as prepara
-- para uso real: a V1 nao declarou nenhuma constraint alem das chaves
-- estrangeiras, e o caso de uso central desta tabela ("este aluno pode
-- treinar aqui hoje?") depende inteiramente delas.
--
-- Como nenhuma tem linha, os ALTER ... SET NOT NULL sao seguros.
-- ==========================================================

-- ==========================================================
-- Planos
-- ==========================================================

-- Plano nao se apaga: ele sai de linha. Assinaturas passadas apontam
-- para ele, e o valor cobrado precisa continuar rastreavel ao plano que
-- o aluno de fato contratou.
ALTER TABLE matriculas.planos
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE matriculas.planos
    ADD CONSTRAINT ck_plano_valor CHECK (valor_mensal > 0),
    ADD CONSTRAINT ck_plano_cobranca
        CHECK (tipo_cobranca IN ('RECORRENTE', 'PACOTE_ANUAL'));

-- Dois planos com o mesmo nome e erro de cadastro: na hora de matricular,
-- a secretaria escolhe pelo nome.
CREATE UNIQUE INDEX IF NOT EXISTS uq_plano_nome
    ON matriculas.planos (lower(nome));

-- ==========================================================
-- Assinaturas
-- ==========================================================

-- Cancelamento auditavel: quando um aluno deixou a academia e a pergunta
-- que a recepcao faz quando ele volta.
ALTER TABLE matriculas.assinaturas_alunos
    ADD COLUMN IF NOT EXISTS data_cancelamento DATE;

ALTER TABLE matriculas.assinaturas_alunos
    ALTER COLUMN aluno_id SET NOT NULL,
    ALTER COLUMN plano_id SET NOT NULL,
    ADD CONSTRAINT ck_assinatura_origem
        CHECK (origem IN ('DIRETO', 'GYMPASS', 'TOTALPASS')),
    ADD CONSTRAINT ck_assinatura_status
        CHECK (status IN ('ATIVA', 'INADIMPLENTE', 'CANCELADA')),
    ADD CONSTRAINT ck_assinatura_periodo
        CHECK (data_vencimento >= data_inicio),
    -- Token de parceiro e o que identifica o aluno no Gympass/TotalPass:
    -- sem ele nao ha como conferir o acesso, e numa matricula direta ele
    -- nao significa nada.
    ADD CONSTRAINT ck_assinatura_token_parceiro CHECK (
        (origem = 'DIRETO' AND token_parceiro IS NULL)
     OR (origem IN ('GYMPASS', 'TOTALPASS') AND token_parceiro IS NOT NULL)
    ),
    -- Data de cancelamento so existe depois de cancelada, e uma
    -- assinatura cancelada precisa dizer quando foi.
    ADD CONSTRAINT ck_assinatura_cancelamento CHECK (
        (status = 'CANCELADA'  AND data_cancelamento IS NOT NULL)
     OR (status <> 'CANCELADA' AND data_cancelamento IS NULL)
    );

-- Um aluno tem no maximo uma matricula vigente.
--
-- Indice parcial sobre o que nao esta cancelado — e nao so sobre ATIVA:
-- uma assinatura INADIMPLENTE continua sendo a matricula do aluno, ele
-- so esta em atraso. Se a restricao valesse so para ATIVA, bastaria
-- atrasar o pagamento para conseguir abrir uma segunda matricula e
-- deixar a primeira para tras. Cancelar libera o aluno para se
-- rematricular, e o historico de canceladas cresce sem limite.
CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_vigente_por_aluno
    ON matriculas.assinaturas_alunos (aluno_id)
    WHERE status <> 'CANCELADA';

-- O mesmo cadastro de parceiro nao pode estar preso a dois alunos: o
-- token e a identidade do usuario la, e duplicado significa que alguem
-- digitou o codigo de outra pessoa.
CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_token_parceiro
    ON matriculas.assinaturas_alunos (origem, token_parceiro)
    WHERE token_parceiro IS NOT NULL AND status <> 'CANCELADA';

-- ==========================================================
-- Indices de apoio as consultas que as telas fazem
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_assinatura_aluno ON matriculas.assinaturas_alunos (aluno_id);
CREATE INDEX IF NOT EXISTS idx_assinatura_status_vencimento
    ON matriculas.assinaturas_alunos (status, data_vencimento);
CREATE INDEX IF NOT EXISTS idx_assinatura_plano ON matriculas.assinaturas_alunos (plano_id);
CREATE INDEX IF NOT EXISTS idx_plano_unidade ON matriculas.plano_unidades (unidade_id);
