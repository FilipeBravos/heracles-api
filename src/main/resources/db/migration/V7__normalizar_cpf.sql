-- ==========================================================
-- Normalizacao de CPF.
--
-- A coluna e UNIQUE, mas "111.222.333-44" e "11122233344" sao strings
-- diferentes para o Postgres, entao a restricao nao impedia o mesmo CPF
-- de entrar duas vezes com pontuacao diferente. A aplicacao passa a gravar
-- apenas digitos; aqui alinhamos as linhas que ja existem.
-- ==========================================================

UPDATE core.usuarios
SET cpf = regexp_replace(cpf, '\D', '', 'g')
WHERE cpf IS DISTINCT FROM regexp_replace(cpf, '\D', '', 'g');
