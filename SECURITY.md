# Security Policy

## Reporting a vulnerability

Do not report security vulnerabilities in public issues. Contact the repository maintainer privately through the GitHub profile and include reproduction steps, impact, and affected versions.

## Security controls

- Passwords are hashed with BCrypt (work factor 12).
- Access tokens are signed with HS512 and require a Base64 secret containing at least 64 random bytes.
- Refresh tokens are random opaque values. Only SHA-256 hashes are persisted.
- Refresh tokens rotate on use. Reuse of an already-rotated token revokes its token family.
- Registration can only assign the USER role. The first ADMIN account is created only through explicit environment configuration.
- API errors do not include stack traces or submitted credentials.
- Database schema changes are managed by Flyway migrations.
- CORS origins are configured explicitly.

## Deployment requirements

Use TLS at the ingress, keep secrets in a managed secret store, rotate the database password, restrict database network access, and configure rate limiting at the gateway or reverse proxy. Do not expose Actuator environment, configuration, or metrics endpoints publicly.
