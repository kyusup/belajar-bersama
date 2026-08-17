# Consistency Checklist — Belajar Bersama

Quality gate artifact (documentation + CI), not runtime enforcement.  
Supersedes the Phase 0–1-only checklist after MVP audit — see [MVP_AUDIT.md](MVP_AUDIT.md).

| Rule | Status | Where aligned |
|---|---|---|
| Product Constitution ↔ implementation | Pass | [MVP_AUDIT.md](MVP_AUDIT.md) |
| Public learn without auth | Pass | Public REST + web routes; `CriticalJourneyResourceTest` |
| Verification-scoped contribution | Pass | `AuthorizationPolicies`, verification workflow |
| Maker–checker, no self-approval | Pass | `MakerCheckerPolicy`, `ContentWorkflowResourceTest` |
| Explicit publish after approval | Pass | Open decision #3 resolved; API enforced |
| Q&A accept: asker or moderator | Pass | Open decision #13; `QaResourceTest` |
| Admin ≠ moderator by default | Pass | `V2` role grants; `IdentityAuthorizationResourceTest` |
| No username/password auth | Pass | OIDC + dev stub only |
| No MVP monetization / AI-as-truth / DM | Pass | Constitution; no code in `apps/` |
| Dev login disabled in production config | Pass | Default false; `DevLoginDisabledResourceTest` |
| Rate limit + CSRF origin | Pass | Filters + integration tests |
| Open decisions not invented | Pass | [OPEN_DECISIONS.md](OPEN_DECISIONS.md) |

## CI quality bar

- Backend: `./mvnw -B verify` (Flyway migrations, 61+ tests)
- Frontend: lint, Prettier, Vitest, build
- E2E: Playwright local only ([E2E_TESTING.md](E2E_TESTING.md))
