CREATE TABLE core.treinos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    foco VARCHAR(50) NOT NULL,
    nivel VARCHAR(50) NOT NULL
);

-- Inserindo alguns dados de teste para o MVP do Heracles
INSERT INTO core.treinos (nome, foco, nivel) VALUES
('Ficha A - Peito e Tríceps', 'Hipertrofia', 'Intermediário'),
('Circuito Funcional', 'Condicionamento', 'Iniciante'),
('Ficha B - Costas e Bíceps', 'Hipertrofia', 'Avançado');