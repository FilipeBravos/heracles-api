-- O sincronismo de fichas (usuario_treinos) sempre foi um "estado atual":
-- trocar a ficha do aluno substitui a linha, sem deixar rastro de qual
-- ficha ele tinha antes nem por quanto tempo. Esta tabela registra cada
-- vinculo como um periodo, para o aluno ver o que treinou antes.
--
-- O nome, o foco e o nivel da ficha sao gravados aqui tambem, e nao lidos
-- de core.treinos na hora de exibir: a ficha pode ser renomeada depois, ou
-- apagada de vez (TreinoService.deletar apaga a linha). O historico
-- descreve o que o aluno treinou naquele periodo, nao o que a ficha e
-- hoje — as duas perguntas podem ter respostas diferentes.
CREATE TABLE core.historico_treinos_aluno (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL,
    treino_id BIGINT,
    treino_nome VARCHAR(100) NOT NULL,
    treino_foco VARCHAR(50),
    treino_nivel VARCHAR(50),
    vinculado_em TIMESTAMP NOT NULL,
    -- Nulo enquanto a ficha ainda esta com o aluno. Vira "atual", nao
    -- "anterior", ate que a proxima troca feche o periodo.
    desvinculado_em TIMESTAMP,
    CONSTRAINT fk_hta_aluno FOREIGN KEY (aluno_id) REFERENCES core.usuarios(id) ON DELETE CASCADE,
    -- SET NULL, nao CASCADE: apagar a ficha nao pode apagar o registro de
    -- que o aluno a treinou. O nome ja esta gravado acima; so a referencia
    -- para o registro vivo se perde.
    CONSTRAINT fk_hta_treino FOREIGN KEY (treino_id) REFERENCES core.treinos(id) ON DELETE SET NULL
);

-- A consulta do historico sempre filtra por aluno e por "ja encerrado"
-- (desvinculado_em IS NOT NULL); o indice cobre as duas.
CREATE INDEX idx_hta_aluno_desvinculado ON core.historico_treinos_aluno (aluno_id, desvinculado_em);
