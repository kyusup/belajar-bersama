# Architecture Decisions — Belajar Bersama

Index of Architecture Decision Records. Full records live in `docs/adr/`.

No prior ADR convention existed; this directory is the project convention going forward.

| ID | Title | Status |
|---|---|---|
| [ADR-001](adr/ADR-001-frontend-architecture.md) | Frontend architecture | Accepted |
| [ADR-002](adr/ADR-002-backend-architecture.md) | Backend architecture | Accepted |
| [ADR-003](adr/ADR-003-authentication-architecture.md) | Authentication architecture | Accepted |
| [ADR-004](adr/ADR-004-database-strategy.md) | Database strategy | Accepted |
| [ADR-005](adr/ADR-005-object-storage-strategy.md) | Object storage strategy | Accepted |
| [ADR-006](adr/ADR-006-search-strategy.md) | Search strategy | Accepted |
| [ADR-007](adr/ADR-007-authorization-strategy.md) | Authorization strategy | Accepted |
| [ADR-008](adr/ADR-008-audit-strategy.md) | Audit strategy | Accepted |
| [ADR-009](adr/ADR-009-monorepo-tooling.md) | Monorepo tooling | Accepted |
| [ADR-010](adr/ADR-010-api-contract-style.md) | API contract style | Accepted |

Related product decisions remain in `OPEN_DECISIONS.md`. ADR-009 and ADR-010 resolve former architecture open decisions #21 and #22.

### Phase 3 documentation corrections

| Topic | What was wrong | Resolution |
|---|---|---|
| API auth header | `API_ARCHITECTURE.md` said `Authorization: Bearer` | Implementation follows ADR-003: cookie `bb_session`. Docs updated. |
| Auth implementation timing | Phase 2 docs said login was future | Phase 3 implements BFF OIDC + session |
| Checker eligibility | Open decision #7 | Resolved: `CHECKER` + approved verification + not maker |

