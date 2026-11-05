# AuthForge

API REST para cadastro de contas, autenticação e administração básica de usuários. Foi construída com Java 21, Spring Boot, Spring Security e PostgreSQL.

O ponto central do projeto é o ciclo de vida dos tokens: o JWT de acesso dura pouco; o refresh token é opaco, armazenado como hash e substituído a cada uso.

## Decisões de segurança

- Senhas são armazenadas com BCrypt (work factor 12).
- Access tokens JWT usam HS512, emissor, expiração e a claim `token_use=access`.
- Cada refresh token é gerado com 64 bytes aleatórios. O banco recebe somente o SHA-256 do valor entregue ao cliente.
- Refresh tokens pertencem a uma família. Ao detectar a reutilização de um token já rotacionado, a API revoga os tokens ainda ativos daquela família.
- O cadastro público sempre cria uma conta `USER`; não existe parâmetro para o cliente escolher `ADMIN`.
- A primeira conta administrativa só é criada quando email e senha são configurados explicitamente no ambiente.

Essas escolhas reduzem alguns riscos comuns, mas não transformam o projeto em um provedor de identidade pronto para produção.

## Endpoints

| Método | Rota | Acesso | Função |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Criar conta e emitir tokens |
| POST | `/api/v1/auth/login` | Público | Autenticar |
| POST | `/api/v1/auth/refresh` | Público, com refresh token | Rotacionar a sessão |
| POST | `/api/v1/auth/logout` | Público, com refresh token | Revogar a família da sessão |
| GET | `/api/v1/users/me` | USER / ADMIN | Consultar a própria conta |
| GET | `/api/v1/admin/users` | ADMIN | Listar contas |
| GET | `/api/v1/admin/users/{id}` | ADMIN | Consultar uma conta |
| PATCH | `/api/v1/admin/users/{id}/status` | ADMIN | Habilitar, desabilitar ou bloquear |

Os endpoints protegidos recebem `Authorization: Bearer <accessToken>`. Refresh e logout recebem `{"refreshToken":"..." }` no corpo JSON.

## Executar localmente

Requisitos: JDK 21 e PostgreSQL. Configure a conexão e gere um segredo JWT antes de iniciar:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/authforge"
$env:DATABASE_USERNAME = "authforge"
$env:DATABASE_PASSWORD = Read-Host "Defina a senha local do banco (nao salve no repositorio)"

$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String(
    [Security.Cryptography.RandomNumberGenerator]::GetBytes(64)
)
$env:JWT_ISSUER = "authforge"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"

# Opcional: configure BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD
# usando variaveis privadas do ambiente, somente durante o provisionamento inicial.

.\gradlew.bat bootRun
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). A migração Flyway é aplicada na inicialização.

## Docker Compose

```powershell
Copy-Item .env.example .env
# Edite .env: defina senhas próprias e JWT_SECRET_BASE64.
docker compose up --build -d
docker compose logs -f api
```

Não versione o arquivo `.env`. A chave precisa decodificar para pelo menos 64 bytes aleatórios.

## Testes

```powershell
.\gradlew.bat clean test
```

A suíte usa H2 em memória e executa a migração Flyway. Os dez testes cobrem cadastro, validação, papéis, login, rotação e reutilização de refresh token, logout e revogação após bloqueio de conta. A execução com PostgreSQL em container ainda precisa ser validada em um ambiente com Docker Engine ativo.

## Limites antes de produção

Ainda não há confirmação de e-mail, recuperação de senha, MFA, rate limiting na aplicação, gestão de dispositivos ou trilha de auditoria imutável. Um access token já emitido continua válido até expirar, mesmo após o usuário ser bloqueado; por isso a duração padrão é de dez minutos. Também são necessários TLS, gestão de segredos, backups testados, monitoramento e testes de carga no ambiente de destino.
