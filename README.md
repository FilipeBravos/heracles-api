# heracles-api

API de gestão para academias e boxes de CrossFit. Spring Boot 4 sobre PostgreSQL,
com migrações Flyway e autenticação por JWT.

## Pré-requisitos

- JDK 17 ou superior
- Docker (para o Postgres de desenvolvimento)

## Subindo o projeto

```bash
# 1. Banco de dados (Postgres 16 na porta 5433)
docker compose up -d

# 2. API (perfil "local" é o padrão)
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. As migrações Flyway rodam
automaticamente no startup e o Hibernate valida o mapeamento contra o schema
resultante — ele nunca cria nem altera tabelas por conta própria.

### Primeiro acesso

No perfil `local`, um administrador é criado na primeira execução caso ainda
não exista:

| E-mail                   | Senha           |
|--------------------------|-----------------|
| `admin@heracles.com.br`  | `Heracles@2026` |

Essas credenciais existem **apenas** no perfil `local`. Em produção o primeiro
administrador é criado por operação.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@heracles.com.br","senha":"Heracles@2026"}'
```

A resposta traz o token; envie-o como `Authorization: Bearer <token>` nas
demais rotas.

## Configuração

O `application.properties` não guarda segredo nenhum: ele lê tudo do ambiente.
O perfil `local` (`application-local.properties`) preenche esses valores para
desenvolvimento.

| Variável               | Descrição                                            |
|------------------------|------------------------------------------------------|
| `DB_URL`               | JDBC URL do Postgres                                  |
| `DB_USERNAME`          | Usuário do banco                                      |
| `DB_PASSWORD`          | Senha do banco                                        |
| `JWT_SECRET`           | Chave HMAC de **no mínimo 32 bytes** para assinar o token |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas, separadas por vírgula             |

Em produção, rode com `SPRING_PROFILES_ACTIVE=prod` e defina as cinco. A
aplicação falha no startup se `JWT_SECRET` for curto demais — é melhor não
subir do que subir assinando token com chave fraca.

## Testes

```bash
./mvnw verify
```

A suíte é composta de slices `@WebMvcTest` e testes unitários, então roda sem
banco. O CI tem um job separado que sobe a aplicação contra um Postgres real
para exercitar as migrações e a validação de schema.

## Perfis de acesso

A autorização é por `TipoPerfil`, aplicada no `SecurityConfig`:

| Rota                          | Quem acessa                          |
|-------------------------------|--------------------------------------|
| `POST /api/auth/login`        | Público                              |
| `GET /api/usuarios`           | ADMIN, SECRETARIA, PROFESSOR         |
| `POST`/`PUT /api/usuarios`    | ADMIN, SECRETARIA                    |
| `PUT /api/usuarios/*/treinos` | ADMIN, SECRETARIA, PROFESSOR         |
| `GET /api/treinos`            | Qualquer usuário autenticado         |
| `POST`/`PUT`/`DELETE /api/treinos` | ADMIN, PROFESSOR                |
| `GET /api/unidades`           | Qualquer usuário autenticado         |
| escrita em `/api/unidades`    | ADMIN                                |

## Estrutura

```
core/
  controller/   camada HTTP, só DTOs — entidades não cruzam essa fronteira
  service/      regras de negócio, com @Transactional
  repository/   Spring Data, com @EntityGraph onde a consulta precisa
  domain/       entidades JPA
  dto/          contratos de entrada e saída
security/       emissão de token e carregamento do usuário
exception/      exceções de domínio e o @RestControllerAdvice
config/         segurança, CORS e o seed de desenvolvimento
```

## Erros

Toda resposta de erro segue o formato `ProblemDetail` (RFC 9457):

```json
{
  "type": "https://heracles.com.br/erros/validacao",
  "title": "Dados invalidos",
  "status": 400,
  "detail": "Um ou mais campos nao passaram na validacao.",
  "erros": { "email": "Informe um e-mail valido" }
}
```
