# AuthForge — Secure Authentication API

API REST de autenticação e gestão de contas construída com Java 21 e Spring Boot 4.1.2. O projeto demonstra autenticação stateless, JWT de curta duração, refresh tokens rotativos, autorização por papéis e persistência PostgreSQL.

> Projeto de portfólio. Antes de uma utilização comercial, revise os requisitos de segurança, privacidade, disponibilidade e operação do ambiente de destino.

## Funcionalidades

- Cadastro e login com validação de entrada e hash de senha BCrypt (work factor 12).
- Access tokens JWT HS512 com emissor, expiração e finalidade validados.
- Refresh tokens opacos de alta entropia; somente o hash SHA-256 é armazenado no banco.
- Rotação de refresh token a cada uso e revogação da família quando ocorre reutilização.
- Logout com revogação da família de sessão.
- Papéis USER e ADMIN; o cadastro público sempre cria USER.
- Criação opcional do primeiro administrador por variáveis de ambiente, sem credenciais padrão.
- Administração para listar contas e habilitar, desabilitar ou bloquear usuários. Desabilitar/bloquear também revoga refresh tokens.
- PostgreSQL com migrações Flyway e perfil de teste H2.
- CORS configurável, respostas de erro padronizadas, Actuator Health e Swagger/OpenAPI.
- Dockerfile com runtime não-root e Docker Compose para API + PostgreSQL.

## Stack

Java 21 · Spring Boot 4.1.2 · Spring Security · Spring Data JPA · PostgreSQL · Flyway · JWT/Nimbus · springdoc-openapi 3.2.0 · Gradle · JUnit 6.

## Executar localmente

Pré-requisitos: JDK 21 e PostgreSQL 15+.

Crie um banco e usuário PostgreSQL para a aplicação. No PowerShell, configure:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/authforge"
$env:DATABASE_USERNAME = "authforge"
$env:DATABASE_PASSWORD = "sua-"

# Gera 64 bytes aleatórios e codifica em Base64. Não reutilize este segredo entre ambientes.
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
$env:JWT_ISSUER = "authforge"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"

# Opcional: configure os dois valores para criar o primeiro administrador.
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@seudominio.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "REMOVED_FROM_PUBLIC_HISTORY"
```

Execute:

```powershell
.gradlew.bat bootRun
```

As migrações são aplicadas automaticamente. API: `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`. OpenAPI JSON: `http://localhost:8080/v3/api-docs`.

## Executar com Docker Compose

```powershell
Copy-Item .env.example .env
# Edite .env e defina senhas fortes, JWT_SECRET_BASE64 e, se aplicável, o administrador inicial.
docker compose up --build -d
docker compose logs -f api
```

Não publique o arquivo `.env`. O segredo JWT precisa decodificar para pelo menos 64 bytes. Use um gerenciador de segredos em ambientes hospedados.

## Endpoints

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Criar conta USER e emitir tokens |
| POST | `/api/v1/auth/login` | Público | Autenticar e emitir tokens |
| POST | `/api/v1/auth/refresh` | Público | Rotacionar refresh token |
| POST | `/api/v1/auth/logout` | Público com refresh token | Revogar sessão |
| GET | `/api/v1/users/me` | USER/ADMIN | Consultar conta autenticada |
| GET | `/api/v1/admin/users` | ADMIN | Listar contas |
| GET | `/api/v1/admin/users/{id}` | ADMIN | Consultar conta |
| PATCH | `/api/v1/admin/users/{id}/status` | ADMIN | Habilitar, desabilitar ou bloquear conta |
| GET | `/actuator/health` | Público | Estado de saúde da aplicação |

Os endpoints de autenticação recebem e retornam JSON. Os endpoints protegidos recebem `Authorization: Bearer <accessToken>`. O refresh token é enviado no corpo JSON como `{"refreshToken":"..."}`.

## Executar os testes

```powershell
.gradlew.bat clean test
```

Os testes usam H2 em memória e aplicam as mesmas migrações Flyway. Não precisam de PostgreSQL em execução. A suíte cobre cadastro, validação, duplicidade case-insensitive, acesso autenticado, papéis, bootstrap admin, rotação e reutilização de refresh token, logout e revogação após bloqueio de conta.

## Modelo de segurança

O access token tem duração padrão de 10 minutos. O refresh token tem duração padrão de 7 dias e é renovado a cada utilização. O banco armazena apenas o SHA-256 do refresh token, não o valor que o cliente apresenta. A detecção de reutilização revoga os tokens não revogados da mesma família.

O segredo JWT não possui valor padrão funcional: sem `JWT_SECRET_BASE64` válido, a aplicação não inicia. Ele deve ser gerado com fonte criptograficamente segura e mantido fora do repositório. O cadastro público nunca permite escolher papéis. O administrador inicial só é criado quando email e senha são fornecidos explicitamente; a senha precisa ter pelo menos 16 caracteres.

## Limitações conhecidas e próximos passos para produção

Esta implementação não inclui fluxo de recuperação de senha, confirmação de email, MFA, gestão de dispositivos, auditoria imutável ou rate limiting interno. Antes de produção, adicione esses controles conforme o risco, TLS na borda, proteção contra abuso no gateway, gestão/rotação de segredos, backups testados, monitoramento, política de retenção e testes de carga. Tokens de acesso já emitidos continuam válidos até expirarem; mantenha a vida útil curta.

## Licença

MIT. Consulte [LICENSE](LICENSE).
