ALTER TABLE core.usuarios ADD COLUMN treino_id BIGINT;

ALTER TABLE core.usuarios
ADD CONSTRAINT fk_usuario_treino
FOREIGN KEY (treino_id) REFERENCES core.treinos(id) ON DELETE SET NULL;