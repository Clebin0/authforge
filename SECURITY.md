# Política de segurança

## Comunicar uma vulnerabilidade

Não publique detalhes de uma vulnerabilidade em uma issue aberta. Entre em contato com o mantenedor pelo perfil do GitHub e inclua os passos para reproduzir o problema, o impacto observado e a versão afetada. Não envie senhas, tokens ou dados pessoais no relato.

## Controles existentes

- Senhas com hash BCrypt (work factor 12).
- JWT de acesso assinado com HS512 e segredo Base64 de pelo menos 64 bytes aleatórios.
- Refresh tokens opacos; somente hashes SHA-256 são persistidos.
- Rotação de refresh token e revogação da família em caso de reutilização.
- Cadastro público restrito ao papel USER.
- Criação de ADMIN somente por configuração explícita de ambiente.
- CORS com origens configuradas; respostas de erro não incluem stack trace nem credenciais.
- Alterações de esquema versionadas com Flyway.

## Antes de expor a API

Use TLS, mantenha os segredos fora do repositório, restrinja o acesso à rede do banco e configure rate limiting no gateway ou proxy reverso. Não exponha endpoints de configuração, ambiente ou métricas do Actuator. Faça testes de restauração dos backups e defina monitoramento e retenção de dados.
