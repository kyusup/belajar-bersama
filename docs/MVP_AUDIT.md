# MVP Audit — Belajar Bersama

Audit date: development snapshot (version `0.1.0`).  
Authority: [PRODUCT_CONSTITUTION.md](PRODUCT_CONSTITUTION.md).  
This is a **local MVP-capable** implementation, **not** a production release. See [RELEASE.md](RELEASE.md).

---

## Verdict

**Pass — MVP constitution compliance.**  
Implementation matches foundational product rules. Remaining gaps are operational (hosting, OIDC production config, backups) and open product decisions in [OPEN_DECISIONS.md](OPEN_DECISIONS.md), not silent policy invention.

---

## Foundational rules (§3)

| Rule | Status | Evidence |
|---|---|---|
| Public learning without auth | Pass | `/api/v1/public/*`; web home, materi, kuis, tanya |
| Quality over quantity | Process | Maker–checker + verification gates |
| Independent learning (no AI dependency) | Pass | No AI tutor or chat in `apps/` |
| Verified contribution only | Pass | `AuthorizationPolicies`, competency verification |
| Reviewed publication | Pass | Submit → review → approve → explicit publish |
| No self-approval | Pass | `MakerCheckerPolicy`; API + tests |
| Privacy by default | Partial | Minimized admin UI; profiles/age-gate open (#9–10) |
| Knowledge before AI | Pass | No AI features |
| Public learning free | Pass | No payments, ads, subscriptions |
| Not school LMS only | Pass | Taxonomy + informal Q&A |

---

## Access model (§4)

| Capability | Expected | Status |
|---|---|---|
| Anonymous public browse | Yes | Pass |
| Progress / bookmarks / quiz history | Auth only | Pass |
| Q&A write | Auth only | Pass |
| Content create | Verified contributor | Pass |
| Content review | Checker (eligible) | Pass |
| Governance | Administrator | Pass |

---

## Security baseline

| Control | Status | Notes |
|---|---|---|
| Google/Apple OIDC BFF + session cookie | Pass | `AuthResource`, `SessionAuthFilter` |
| Dev login off by default | Pass | `.env.example`, `DEV_LOGIN_DISABLED` test |
| Rate limiting | Pass | In-process filter; `429` integration test |
| CSRF Origin/Referer check | Pass | `MutatingOriginFilter` |
| Content sanitization | Pass | `ContentSanitizer` on create/review/Q&A |
| Web CSP / Permissions-Policy | Pass | `next.config.ts` |

---

## Feature completeness (MVP scope)

| Area | Status |
|---|---|
| Identity, RBAC, verification | Implemented |
| Educational content + maker–checker | Implemented |
| Learning (progress, bookmarks, quiz) | Implemented |
| Q&A + moderation | Implemented |
| Admin console (`/kelola`) | Implemented |
| Search (Postgres) | Implemented |
| CI (backend verify + web lint/test/build) | Implemented |
| API golden-path test | `CriticalJourneyResourceTest` |
| Browser E2E (Playwright, local) | `apps/web/e2e` |

**Intentionally absent:** production deploy, AI tutor, DMs, payments, username/password auth, admin force-publish (open #1).

---

## Test inventory

| Layer | Count | Command |
|---|---|---|
| Backend integration + unit | 61+ | `cd apps/api && ./mvnw verify` |
| Frontend unit (Vitest) | 7 | `pnpm --filter web test` |
| Browser E2E (Playwright) | 6 | `pnpm test:e2e` (local stack required) |

---

## Open decisions (do not implement without product call)

- Admin force-publish (#1)
- Public contributor profiles (#9)
- Age-gate / parental consent (#10)
- Production hosting (#4 in RELEASE.md)

---

## Before production

Follow [RELEASE.md](RELEASE.md): disable dev login, production OIDC, secure cookies, secrets management, backups, multi-instance rate-limit store review.

---

## Document alignment

Living docs updated in Phase 9 audit: `ARCHITECTURE.md`, `API_ARCHITECTURE.md`, `DEVELOPMENT_SETUP.md`, `DEPLOYMENT_ARCHITECTURE.md`, `SECURITY_ARCHITECTURE.md`, `CONSISTENCY_CHECKLIST.md`.  
ADRs under `docs/adr/` retain historical Phase 2 context where noted.
