-- ==========================================================
-- Cobranca simulada e regua de inadimplencia.
--
-- Nao ha gateway de pagamento integrado: a cobranca gerada aqui e um
-- registro interno (boleto/PIX/cartao "de mentira"), pago por uma acao
-- da secretaria em vez de um webhook de verdade. O objetivo e ter o
-- historico de cobranca por ciclo pronto para, no dia em que um gateway
-- real entrar, plugar por baixo sem redesenhar o modelo.
-- ==========================================================

-- Forma de pagamento escolhida pelo aluno na matricula, reaproveitada em
-- toda cobranca gerada para aquela assinatura. Default so para as
-- assinaturas que ja existiam antes desta coluna nascer.
ALTER TABLE matriculas.assinaturas_alunos
    ADD COLUMN IF NOT EXISTS forma_pagamento VARCHAR(20) NOT NULL DEFAULT 'PIX';

ALTER TABLE matriculas.assinaturas_alunos
    ADD CONSTRAINT ck_assinatura_forma_pagamento
        CHECK (forma_pagamento IN ('BOLETO', 'PIX', 'CARTAO'));

CREATE TABLE matriculas.cobrancas (
    id BIGSERIAL PRIMARY KEY,
    assinatura_id BIGINT NOT NULL REFERENCES matriculas.assinaturas_alunos(id),
    valor DECIMAL(10,2) NOT NULL CHECK (valor > 0),
    forma_pagamento VARCHAR(20) NOT NULL CHECK (forma_pagamento IN ('BOLETO', 'PIX', 'CARTAO')),
    data_vencimento DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'PAGA', 'CANCELADA')),
    data_pagamento DATE,
    -- Boleto/PIX "copia e cola" fake, so para a tela ter o que mostrar.
    -- Nulo no cartao, que nao tem codigo copiavel.
    codigo_simulado VARCHAR(60),
    data_criacao TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_cobranca_pagamento CHECK (
        (status = 'PAGA' AND data_pagamento IS NOT NULL)
     OR (status <> 'PAGA' AND data_pagamento IS NULL)
    )
);

-- No maximo uma cobranca em aberto por assinatura: a proxima so nasce
-- quando esta e paga (renovar empurra o vencimento e cria a do proximo
-- ciclo) ou cancelada (a assinatura foi cancelada primeiro).
CREATE UNIQUE INDEX uq_cobranca_pendente_por_assinatura
    ON matriculas.cobrancas (assinatura_id)
    WHERE status = 'PENDENTE';

CREATE INDEX idx_cobranca_assinatura ON matriculas.cobrancas (assinatura_id);
CREATE INDEX idx_cobranca_status_vencimento ON matriculas.cobrancas (status, data_vencimento);
