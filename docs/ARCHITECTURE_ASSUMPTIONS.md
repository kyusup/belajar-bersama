# Architecture Assumptions — Belajar Bersama

Phase 0/1 assumptions. **Phase 2 living architecture:** `ARCHITECTURE.md`.

This file is retained for history. Where it conflicts with Phase 2 ADRs, the ADRs win.

---

## 1. Repository Inspection Result

At Phase 0/1 kickoff the repository was **empty**.  
Phase 2 replaced scaffold READMEs with an executable Next.js + Quarkus + PostgreSQL skeleton.

---

## 2. Target Technology Stack

| Layer | Choice |
|---|---|
| Web application | Next.js + TypeScript |
| API / domain services | Java 21 + Quarkus modular monolith |
| Primary database | PostgreSQL 16 |
| Local infrastructure | Docker Compose (Postgres + MinIO) |

---

## 3. What changed in Phase 2

- pnpm workspaces (`apps/web`, `packages/shared`)
- Maven Wrapper in `apps/api`
- Compose moved to repository root
- Code-first OpenAPI
- Domain/application/infrastructure/interfaces packages
