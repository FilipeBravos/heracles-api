-- ==========================================================
-- Contrato digital assinado no cadastro do aluno.
--
-- Substitui "so senha inicial" como o momento de aceite: a senha
-- autentica quem ele e depois, mas nunca disse que ele concordou com
-- os termos. Uma linha por aluno, nunca reescrita — o que foi assinado
-- fica gravado por inteiro em texto_contrato, independente do modelo
-- de contrato mudar depois.
-- ==========================================================

create table core.contratos_assinados (
    id bigserial primary key,
    aluno_id bigint not null references core.usuarios(id),
    nome_digitado varchar(100) not null,
    texto_contrato text not null,
    assinado_em timestamp not null,
    ip_origem varchar(45),
    constraint uk_contrato_aluno unique (aluno_id)
);
