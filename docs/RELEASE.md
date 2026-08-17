# Release — Belajar Bersama

This is **not** a production release.

The local stack plus CI is the current quality bar. A passing `./mvnw verify` and web build does **not** mean production readiness.

**MVP audit:** [MVP_AUDIT.md](MVP_AUDIT.md) (constitution cross-check, Aug 2026 development snapshot).

---

## Current quality bar

| Area | Status |
|---|---|
| Build | API `./mvnw verify`, web `pnpm lint` / `pnpm test` / `pnpm build` |
| CI | GitHub Actions: backend verify + web lint/test/build |
| Migrations | Flyway `V1`–`V5`, migrate-at-start |
| Auth | Google/Apple OIDC + local `dev` login (dev login must stay off in production) |
| Public learning | Published content and non-hidden Q&A readable anonymously |
| Maker–checker | Enforced on the API |
| Quiz scoring | Server-side, immutable attempts |
| Security | Rate limits, Origin/Referer CSRF check, content sanitization, web CSP |
| Tests | 61+ backend tests, API golden-path journey, 7 web unit tests, Playwright E2E (local) |
| Docs | See `docs/README.md`, [E2E_TESTING.md](E2E_TESTING.md) |

---

## Before a real production cut

1. Disable `AUTH_DEV_LOGIN`.
2. Configure Google and Apple OIDC with production redirect URIs.
3. Set `AUTH_COOKIE_SECURE=true`, production `CORS_ORIGINS`, and secrets outside git.
4. Choose hosting (still an open operational decision).
5. Automate PostgreSQL and object-storage backups and test a restore (`docs/BACKUP.md`).
6. Confirm rate-limit values and whether a shared store is needed for multiple API instances.
7. Confirm no seed/demo users are treated as real verified experts.
8. Resolve or explicitly defer open product decisions in `docs/OPEN_DECISIONS.md` that affect launch.

---

## Version

Application version: `0.1.0` (see `apps/api`, `apps/web`, and [CHANGELOG.md](../CHANGELOG.md)).
