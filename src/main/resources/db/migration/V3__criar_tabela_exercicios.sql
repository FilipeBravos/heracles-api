CREATE TABLE core.exercicios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    repeticoes VARCHAR(50) NOT NULL,
    observacoes TEXT,
    treino_id BIGINT NOT NULL,
    CONSTRAINT fk_exercicio_treino FOREIGN KEY (treino_id) REFERENCES core.treinos(id) ON DELETE CASCADE
);

-- Inserindo exercícios de teste para o Treino ID 1 (Ficha A - Peito e Tríceps)
INSERT INTO core.exercicios (nome, repeticoes, observacoes, treino_id) VALUES
('Supino Reto com Barra', '4x10 a 12', 'Descanso de 60s a 90s', 1),
('Crucifixo Inclinado com Halteres', '3x12', 'Focar no alongamento do peitoral', 1),
('Tríceps Pulley na Polia', '4x15', 'Segurar 1s na contração máxima', 1);