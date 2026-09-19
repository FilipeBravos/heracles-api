-- ==========================================================
-- Agenda de aulas/personal: horario semanal do professor, aula em
-- grupo com vagas e sessao individual de personal.
--
-- Schema proprio, como matriculas e operacoes: e um dominio a parte,
-- nao um apendice do cadastro de usuario.
-- ==========================================================

create schema if not exists agenda;

-- ==========================================================
-- Disponibilidade semanal do professor
-- ==========================================================
create table agenda.horarios_professor (
    id bigserial primary key,
    professor_id bigint not null references core.usuarios(id),
    unidade_id bigint not null references core.unidades(id),
    dia_semana varchar(20) not null
        check (dia_semana in ('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO')),
    hora_inicio time not null,
    hora_fim time not null
);
create index idx_horario_professor on agenda.horarios_professor(professor_id, dia_semana);

-- ==========================================================
-- Aula em grupo e inscricoes
-- ==========================================================
create table agenda.aulas_grupo (
    id bigserial primary key,
    nome varchar(100) not null,
    professor_id bigint not null references core.usuarios(id),
    unidade_id bigint not null references core.unidades(id),
    data_hora timestamp not null,
    duracao_minutos integer not null,
    capacidade_maxima integer not null,
    status varchar(20) not null check (status in ('ATIVA','CANCELADA'))
);
create index idx_aula_professor on agenda.aulas_grupo(professor_id, status);
create index idx_aula_data on agenda.aulas_grupo(status, data_hora);

create table agenda.inscricoes_aula (
    id bigserial primary key,
    aula_id bigint not null references agenda.aulas_grupo(id),
    aluno_id bigint not null references core.usuarios(id),
    status varchar(20) not null check (status in ('INSCRITA','CANCELADA')),
    inscrito_em timestamp not null,
    cancelado_em timestamp
);
create index idx_inscricao_aula on agenda.inscricoes_aula(aula_id, status);
create index idx_inscricao_aluno on agenda.inscricoes_aula(aluno_id, status);

-- ==========================================================
-- Sessao de personal
-- ==========================================================
create table agenda.sessoes_personal (
    id bigserial primary key,
    aluno_id bigint not null references core.usuarios(id),
    professor_id bigint not null references core.usuarios(id),
    unidade_id bigint not null references core.unidades(id),
    data_hora timestamp not null,
    duracao_minutos integer not null,
    observacoes varchar(500),
    status varchar(20) not null check (status in ('AGENDADO','CANCELADO'))
);
create index idx_sessao_professor on agenda.sessoes_personal(professor_id, status);
create index idx_sessao_aluno on agenda.sessoes_personal(aluno_id);
