-- Remove o vínculo antigo (1 treino só)
ALTER TABLE core.usuarios DROP CONSTRAINT IF EXISTS fk_usuario_treino;
ALTER TABLE core.usuarios DROP COLUMN IF EXISTS treino_id;

-- Cria a tabela de junção permitindo múltiplos treinos por aluno
CREATE TABLE core.usuario_treinos (
    usuario_id BIGINT NOT NULL,
    treino_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, treino_id),
    CONSTRAINT fk_ut_usuario FOREIGN KEY (usuario_id) REFERENCES core.usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_ut_treino FOREIGN KEY (treino_id) REFERENCES core.treinos(id) ON DELETE CASCADE
);