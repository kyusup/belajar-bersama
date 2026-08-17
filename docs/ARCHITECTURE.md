# Architecture — Belajar Bersama

Living technical architecture. Product authority remains `PRODUCT_CONSTITUTION.md`. Domain authority remains `DOMAIN_MODEL.md`.

This document describes architecture that exists in the repository at MVP `0.1.0`. It does **not** claim production readiness or a hosted deploy.

---

## 1. Current system

```text
Browser → Next.js (apps/web) → Quarkus API (apps/api) → PostgreSQL
                                      ↓
                              Object storage (MinIO / memory)
```

Implemented capabilities (Phases 3–8):

- Identity: Google/Apple OIDC BFF, session cookie `bb_session`, RBAC, competency verification
- Content: taxonomy, revisions, maker–checker, publication, content reports
- Learning: browse, progress, bookmarks, resume, server-side quiz scoring
- Community: public Q&A, moderation (hide-not-delete), search
- Admin: verification review, roles, taxonomy, user directory (display name only)
- Security: rate limits, Origin/Referer CSRF check, content sanitization

Public learning endpoints remain anonymous. Protected operations require authentication on the API.

---

## 2. Repository structure

```text
belajar-bersama/
├── apps/
│   ├── web/                 # Next.js + TypeScript UI
│   └── api/                 # Quarkus modular monolith
├── packages/
│   └── shared/              # Shared TypeScript contracts (API types, permissions)
├── docs/
│   └── adr/                 # Architecture Decision Records
├── infra/
│   ├── docker/              # Container build files
│   └── postgres/            # Optional DB init notes
├── scripts/
├── .github/workflows/
├── docker-compose.yml       # Local infrastructure (Postgres, MinIO)
├── pnpm-workspace.yaml
├── package.json
├── README.md
└── LICENSE
```

**Structure decision:** keep the Phase 1 `apps/` + `docs/` + `packages/` layout. Move Compose to the repository root (developer clone-and-run). Place Dockerfiles under `infra/docker/` and PostgreSQL notes under `infra/postgres/`. This matches the Phase 2 request without discarding Phase 1 conventions.

---

## 3. Runtime topology (local)

| Process | Role | Default |
|---|---|---|
| `apps/web` | Presentation | `http://localhost:3000` |
| `apps/api` | Domain + application + persistence authority | `http://localhost:8080` |
| PostgreSQL 16 | System of record | `localhost:55432` |
| MinIO | Local S3-compatible object storage | `localhost:9000` |

The API is the only enforcement point for authorization, verification, publication, and audit. The web app must not be treated as a source of business rules.

---

## 4. Backend module boundaries

Package root: `id.belajarbersama`

| Layer | Package | May depend on | Must not depend on |
|---|---|---|---|
| Domain | `domain` | JDK + domain only | Quarkus persistence, HTTP, S3 SDK |
| Application | `application` | domain | infrastructure implementations, JAX-RS |
| Interfaces | `interfaces` | application, domain | JDBC/S3 implementations |
| Infrastructure | `infrastructure` | domain, application (ports) | (implements ports) |

Dependency rule: **domain does not import infrastructure**. Infrastructure implements domain/application ports via CDI.

This is a **modular monolith** (one Quarkus deployable), consistent with Phase 1 assumptions.

---

## 5. Frontend boundaries

| Area | Location | Responsibility |
|---|---|---|
| Presentation / routes | `apps/web/src/app` | Pages, layouts, Indonesian UI |
| Shared UI | `apps/web/src/components` | Reusable presentational components |
| API communication | `apps/web/src/lib/api` | HTTP client, endpoint calls |
| Authentication state | `apps/web/src/lib/auth` | Convenience only; API cookie is authority |
| Domain-facing types | `packages/shared` + `apps/web/src/types` | DTOs and permission names |

React components must not encode Maker–Checker, verification, or publication invariants. UI may hide actions for convenience later; the API must still reject illegal actions.

---

## 6. Identity model

```text
Identity Provider (Google / Apple)
        ↓
identity_link  (external subject, private)
        ↓
app_user
        ↓
Roles / Verification / Permissions
```

OAuth subject IDs are **not** the domain `User`. See ADR-003, `IDENTITY_ARCHITECTURE.md`, and `AUTHENTICATION.md`.

---

## 7. Persistence

- PostgreSQL is the transactional system of record.
- Schema evolution: **Flyway** (`apps/api/src/main/resources/db/migration`, currently `V1`–`V5`).
- Identifiers: UUID.
- Audit: dedicated `audit_event` table (append-oriented).
- Educational content, learning progress, Q&A, and search index tables are migrated and in use.

---

## 8. Ports (abstractions)

| Port | Purpose | Adapter |
|---|---|---|
| `ObjectStorage` | S3-compatible blob storage | S3 adapter + in-memory for tests |
| `SearchIndex` | Content + Q&A search | PostgreSQL full-text union |
| `AuditRecorder` | Governance/security audit | PostgreSQL `audit_event` |

---

## 9. Public API surface

Anonymous (published learning):

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/public/content/*` | Published educational content |
| GET | `/api/v1/public/quizzes/*` | Published quizzes (no answer keys) |
| GET | `/api/v1/public/qa/*` | Non-hidden Q&A |
| GET | `/api/v1/public/search` | Cross-type search |
| GET | `/api/v1/health`, `/api/v1/status` | Health and component status |
| GET | `/api/v1/auth/config` | Enabled login providers |
| GET | `/api/v1/competencies`, subjects, education levels | Taxonomy catalog |

Authenticated: `/api/v1/me/*`, content workflow, reviews, verifications, admin, moderation. See [API_ARCHITECTURE.md](API_ARCHITECTURE.md).

---

## 10. What is intentionally absent

Production hosting pipeline, AI tutoring, payments, advertising, recommendation engines, username/password auth, automatic Google+Apple account merge, admin force-publish (open decision #1).

