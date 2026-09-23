-- ==========================================================
-- O que o aluno realmente executou de um exercicio, por data.
--
-- A ficha (core.exercicios) so guarda a prescricao — series,
-- repeticoes e uma carga em texto livre ("ate a falha", "70% 1RM"),
-- compartilhada entre todos os alunos que treinam com ela. Nao havia
-- nenhum registro do que cada aluno de fato fez.
--
-- exercicio_nome vem copiado no momento do registro, e a referencia e
-- opcional: o professor pode remover o exercicio da ficha
-- (TreinoService.atualizar tem orphanRemoval) sem apagar o historico
-- do aluno — o mesmo raciocinio de historico_treinos_aluno.
-- ==========================================================

CREATE TABLE core.execucoes_exercicio (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL,
    exercicio_id BIGINT,
    exercicio_nome VARCHAR(100) NOT NULL,
    data_execucao DATE NOT NULL,
    series_realizadas INT NOT NULL,
    repeticoes_realizadas INT NOT NULL,
    carga_realizada NUMERIC(6, 2),
    observacao VARCHAR(500),
    registrado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_execucao_aluno FOREIGN KEY (aluno_id) REFERENCES core.usuarios(id) ON DELETE CASCADE,
    -- SET NULL, nao CASCADE: apagar o exercicio da ficha nao pode apagar o
    -- que o aluno ja registrou ter feito. O nome ja esta gravado acima.
    CONSTRAINT fk_execucao_exercicio FOREIGN KEY (exercicio_id) REFERENCES core.exercicios(id) ON DELETE SET NULL
);

-- A consulta de evolucao sempre filtra por aluno e exercicio, ordenando por data.
CREATE INDEX idx_execucao_aluno_exercicio ON core.execucoes_exercicio (aluno_id, exercicio_id, data_execucao);
