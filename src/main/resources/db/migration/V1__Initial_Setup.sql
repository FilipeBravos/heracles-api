-- ==========================================
-- 1. CRIAÇÃO DOS SCHEMAS (Módulos do Sistema)
-- ==========================================
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS matriculas;
CREATE SCHEMA IF NOT EXISTS treinos;
CREATE SCHEMA IF NOT EXISTS operacoes;

-- ==========================================
-- 2. SCHEMA CORE (Identidade e Estrutura)
-- ==========================================

CREATE TABLE core.unidades (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- ACADEMIA, CROSSFIT
    endereco VARCHAR(255),
    telefone VARCHAR(20)
);

CREATE TABLE core.usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    telefone VARCHAR(20),
    senha_hash VARCHAR(255) NOT NULL,
    tipo_perfil VARCHAR(20) NOT NULL, -- ALUNO, PROFESSOR, SECRETARIA, ADMIN
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' -- ATIVO, INATIVO
);

-- ==========================================
-- 3. SCHEMA MATRICULAS (Planos e Acessos)
-- ==========================================

CREATE TABLE matriculas.planos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    valor_mensal DECIMAL(10,2) NOT NULL,
    tipo_cobranca VARCHAR(20) NOT NULL -- RECORRENTE, PACOTE_ANUAL
);

-- Tabela de relacionamento N:N (Quais unidades o plano dá acesso)
CREATE TABLE matriculas.plano_unidades (
    plano_id BIGINT REFERENCES matriculas.planos(id) ON DELETE CASCADE,
    unidade_id BIGINT REFERENCES core.unidades(id) ON DELETE CASCADE,
    PRIMARY KEY (plano_id, unidade_id)
);

CREATE TABLE matriculas.assinaturas_alunos (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT REFERENCES core.usuarios(id) ON DELETE CASCADE,
    plano_id BIGINT REFERENCES matriculas.planos(id),
    origem VARCHAR(20) NOT NULL, -- DIRETO, GYMPASS, TOTALPASS
    token_parceiro VARCHAR(100), -- ID do Gympass, se aplicável
    data_inicio DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    status VARCHAR(20) NOT NULL -- ATIVA, INADIMPLENTE, CANCELADA
);

-- ==========================================
-- 4. SCHEMA TREINOS (Musculação e CrossFit)
-- ==========================================

CREATE TABLE treinos.fichas_treino (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT REFERENCES core.usuarios(id) ON DELETE CASCADE,
    professor_id BIGINT REFERENCES core.usuarios(id),
    objetivo VARCHAR(100), -- Ex: Hipertrofia, Emagrecimento
    data_criacao DATE NOT NULL,
    data_vencimento DATE NOT NULL
);

CREATE TABLE treinos.exercicios_ficha (
    id BIGSERIAL PRIMARY KEY,
    ficha_id BIGINT REFERENCES treinos.fichas_treino(id) ON DELETE CASCADE,
    nome_exercicio VARCHAR(100) NOT NULL,
    series INT NOT NULL,
    repeticoes INT NOT NULL,
    carga VARCHAR(50) -- VARCHAR para aceitar textos como "Ate a falha" ou "20kg"
);

CREATE TABLE treinos.aulas_crossfit (
    id BIGSERIAL PRIMARY KEY,
    unidade_id BIGINT REFERENCES core.unidades(id) ON DELETE CASCADE,
    professor_id BIGINT REFERENCES core.usuarios(id),
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    limite_vagas INT NOT NULL
);

CREATE TABLE treinos.agendamentos_crossfit (
    id BIGSERIAL PRIMARY KEY,
    aula_id BIGINT REFERENCES treinos.aulas_crossfit(id) ON DELETE CASCADE,
    aluno_id BIGINT REFERENCES core.usuarios(id) ON DELETE CASCADE,
    status_presenca VARCHAR(20) NOT NULL -- AGENDADO, PRESENTE, FALTA, CANCELADO
);

-- ==========================================
-- 5. SCHEMA OPERACOES (PDV e Manutenção)
-- ==========================================

CREATE TABLE operacoes.produtos_suplementos (
    id BIGSERIAL PRIMARY KEY,
    unidade_id BIGINT REFERENCES core.unidades(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    marca VARCHAR(100),
    preco_venda DECIMAL(10,2) NOT NULL,
    quantidade_estoque INT NOT NULL DEFAULT 0
);

CREATE TABLE operacoes.vendas_pdv (
    id BIGSERIAL PRIMARY KEY,
    unidade_id BIGINT REFERENCES core.unidades(id) ON DELETE CASCADE,
    secretaria_id BIGINT REFERENCES core.usuarios(id),
    aluno_id BIGINT REFERENCES core.usuarios(id), -- Opcional (venda para visitante)
    valor_total DECIMAL(10,2) NOT NULL,
    data_venda TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_pagamento VARCHAR(20) NOT NULL -- PIX, CREDITO, DEBITO, DINHEIRO
);

CREATE TABLE operacoes.itens_venda (
    id BIGSERIAL PRIMARY KEY,
    venda_id BIGINT REFERENCES operacoes.vendas_pdv(id) ON DELETE CASCADE,
    produto_id BIGINT REFERENCES operacoes.produtos_suplementos(id),
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL
);

CREATE TABLE operacoes.equipamentos (
    id BIGSERIAL PRIMARY KEY,
    unidade_id BIGINT REFERENCES core.unidades(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    status_atual VARCHAR(20) NOT NULL -- OK, EM_MANUTENCAO
);

CREATE TABLE operacoes.historico_manutencao (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT REFERENCES operacoes.equipamentos(id) ON DELETE CASCADE,
    data_chamado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descricao_problema TEXT NOT NULL,
    custo_reparo DECIMAL(10,2),
    status VARCHAR(20) NOT NULL -- ABERTO, RESOLVIDO
);