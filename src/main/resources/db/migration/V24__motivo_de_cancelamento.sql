-- ==========================================================
-- Motivo do cancelamento: por que o aluno saiu, capturado pela
-- secretaria no proprio ato de cancelar — nao uma pesquisa enviada
-- depois, que dificilmente alguem que ja saiu responderia.
--
-- Mesma logica de ck_assinatura_cancelamento (V10): o motivo so existe
-- quando a assinatura esta CANCELADA. O comentario e sempre opcional,
-- em qualquer status.
-- ==========================================================

ALTER TABLE matriculas.assinaturas_alunos
    ADD COLUMN motivo_cancelamento VARCHAR(20),
    ADD COLUMN comentario_cancelamento VARCHAR(500);

-- Assinaturas ja canceladas antes desta coluna existir nao tem como saber
-- o motivo de verdade — cai em OUTRO para nao violar a constraint abaixo.
UPDATE matriculas.assinaturas_alunos
    SET motivo_cancelamento = 'OUTRO'
    WHERE status = 'CANCELADA';

ALTER TABLE matriculas.assinaturas_alunos
    ADD CONSTRAINT ck_assinatura_motivo_cancelamento CHECK (
        motivo_cancelamento IS NULL
        OR motivo_cancelamento IN ('PRECO', 'MUDANCA', 'INSATISFACAO', 'FALTA_TEMPO', 'SAUDE', 'CONCORRENCIA', 'OUTRO')
    ),
    ADD CONSTRAINT ck_assinatura_cancelamento_motivo CHECK (
        (status = 'CANCELADA'  AND motivo_cancelamento IS NOT NULL)
     OR (status <> 'CANCELADA' AND motivo_cancelamento IS NULL)
    );
