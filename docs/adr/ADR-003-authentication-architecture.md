# ADR-003: Authentication Architecture

## Status

Accepted and implemented in Phase 3

## Context

Initial product auth is Google Sign-In and Apple Sign-In only. Public learning must work anonymously. Identity providers must not be the domain user.

## Decision

```text
Identity Provider → Identity → Application User → Roles / Verification / Permissions
```

- Support OIDC providers behind an identity gateway in infrastructure (not domain services)
- Persist `identity_link` (issuer, subject, provider) separately from `app_user`
- No username/password
- BFF + HTTP-only session cookie `bb_session` (not bearer tokens in the browser)

## Alternatives

- Username/password: forbidden by constitution for the initial version
- Treating Google `sub` as `User.id`: couples the domain to one provider and complicates Apple linking
- JWT-only in the browser: simpler, higher XSS token theft risk

## Consequences

- Multi-provider merge remains an open product decision (#16)
- Local development may use `AUTH_DEV_LOGIN` when provider credentials are absent
- See `AUTHENTICATION.md` and `IDENTITY_ARCHITECTURE.md`
