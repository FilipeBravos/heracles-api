-- Cadastro completo do aluno: endereco, contato, data de nascimento, foto
-- e o plano que ele escolheu na recepcao. So o aluno usa estes campos de
-- fato — professor, secretaria e admin continuam com nome, cpf, e-mail e
-- telefone, que ja bastam para uma conta de acesso.
ALTER TABLE core.usuarios
    ADD COLUMN endereco VARCHAR(255),
    ADD COLUMN cep VARCHAR(9),
    ADD COLUMN data_nascimento DATE,
    -- Foto guardada no proprio banco: nao ha um object storage no projeto,
    -- e o volume (uma foto de perfil por aluno) nao justifica introduzir
    -- um agora. content_type acompanha os bytes para a resposta HTTP
    -- devolver o Content-Type certo.
    ADD COLUMN foto BYTEA,
    ADD COLUMN foto_content_type VARCHAR(50),
    -- So a escolha, sem virar assinatura: o plano efetivo continua
    -- nascendo pela tela de Matriculas, quando a secretaria confirma.
    -- ON DELETE SET NULL, nao CASCADE — o plano pode sair de linha depois
    -- sem apagar o cadastro do aluno que o escolheu.
    ADD COLUMN plano_escolhido_id BIGINT REFERENCES matriculas.planos(id) ON DELETE SET NULL;

-- Anamnese: tabela a parte, nao colunas em usuarios. A pergunta que a
-- regra de negocio faz e "existe anamnese para este aluno", nao "os
-- campos estao todos preenchidos" — campos de condicao de saude vazios
-- sao uma resposta valida (o aluno nao tem a condicao), nao "ainda nao
-- respondeu". A existencia da linha e o unico sinal confiavel.
CREATE TABLE core.anamneses (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL UNIQUE REFERENCES core.usuarios(id) ON DELETE CASCADE,
    objetivo VARCHAR(500) NOT NULL,
    condicoes_saude TEXT,
    lesoes_cirurgias TEXT,
    medicamentos_uso TEXT,
    restricoes_medicas TEXT,
    contato_emergencia_nome VARCHAR(100) NOT NULL,
    contato_emergencia_telefone VARCHAR(20) NOT NULL,
    preenchida_em TIMESTAMP NOT NULL
);
