# Multi-agent architecture

## Roles
- Security reviewer: compares documented controls to the current implementation.
- Token lifecycle explainer: explains access/refresh rotation and revocation from code-backed facts.
- Threat-model assistant: drafts scenarios and mitigations for human review.
- API documentation assistant: answers from OpenAPI and repository documentation.

## Boundaries
Advisory and read-only. Agents cannot register users, change roles, disable accounts, issue/revoke tokens, alter secrets, or change security configuration. Spring Security and application code remain authoritative for authorization. Never pass JWTs, refresh tokens, passwords, or signing keys into prompts or agent memory.

## Acceptance
Every finding references an endpoint, source file, or test. Findings distinguish implemented controls from missing ones and require human review before code changes.
