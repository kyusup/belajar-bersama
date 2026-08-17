# Deployment Architecture — Belajar Bersama

Local, reproducible deployment shape for MVP development. There is **no production deploy pipeline** yet — see [RELEASE.md](RELEASE.md) and [MVP_AUDIT.md](MVP_AUDIT.md).

---

## 1. What exists

- `docker-compose.yml` at repository root for local Postgres and MinIO
- Application processes run on the developer machine (`quarkus:dev`, `pnpm --filter web dev`)
- GitHub Actions CI verifies build/test/lint/format — it does not deploy

---

## 2. Local topology

```text
Developer browser
      │
      ▼
Next.js :3000  ──HTTP──►  Quarkus :8080  ──JDBC──►  PostgreSQL (host 55432 → container 5432)
                                │
                                └──S3 API──► MinIO :9000
```

CORS: web origin → API. The browser calls the API directly using `NEXT_PUBLIC_API_URL`.

---

## 3. Configuration

Environment-specific values are externalized (see `.env.example`):

- `DATABASE_URL` / discrete `POSTGRES_*`
- `CORS_ORIGINS`
- `APPLICATION_URL`, `API_URL`, `NEXT_PUBLIC_API_URL`
- OIDC placeholders (unused until login phase)
- `S3_*` storage settings

Secrets must not be baked into images or committed.

---

## 4. Containers (later)

Dockerfiles under `infra/docker/` are reserved for API/web images. They are not required for the local developer loop.

When added, expected services:

| Service | Image purpose |
|---|---|
| `web` | Next.js production server or static+asset host |
| `api` | Quarkus JVM (or native, later decision) |
| `postgres` | Official PostgreSQL |
| `minio` or managed S3 | Object storage |

---

## 5. Environments (future)

| Environment | Intent |
|---|---|
| local | Compose + dev servers |
| CI | Ephemeral Postgres via Quarkus Dev Services / Compose |
| production | `OPEN DECISION` (single VM, Kubernetes, or managed PaaS) |

Do not assume Kubernetes. Do not add deploy workflows until hosting exists.

---

## 6. Health for orchestration

When containers are introduced:

- Liveness: `/q/health/live` or `/api/v1/health`
- Readiness: `/q/health/ready` (database)

Load balancers should not send traffic until readiness succeeds.

---

## 7. Backups (future)

PostgreSQL is the system of record. Object storage holds learning binaries. Backup strategy is `OPEN DECISION` and not implemented.
