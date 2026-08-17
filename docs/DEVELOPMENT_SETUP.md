# Development Setup — Belajar Bersama

Local environment for the MVP stack (identity through moderation, admin, and security hardening).

Required on the host:

| Tool | Version used in this phase |
|---|---|
| JDK | 21 |
| Maven | 3.9+ (wrapper provided in `apps/api`) |
| Node.js | 22 |
| pnpm | 11 (workspace `allowBuilds` required) |
| Docker + Compose | 24+ |

---

## 1. Clone and configure

```bash
git clone git@github.com:kyusup/belajar-bersama.git
cd belajar-bersama
cp .env.example .env
```

Edit `.env` only if ports or local credentials must differ. Do not commit `.env`.

The example file contains **local development placeholders**, not production secrets.

---

## 2. Start infrastructure

### Option A — Full stack in Docker (recommended)

```bash
cp .env.docker.example .env.docker
docker compose up -d --build
```

See [docs/DOCKER.md](DOCKER.md) for service URLs, Appium, and optional Android emulator profile.

### Option B — Infrastructure only (apps on host)

From the repository root:

```bash
docker compose up -d
docker compose ps
```

This starts:

- PostgreSQL 16 on `localhost:55432` (host port; container still uses 5432)
- MinIO (S3-compatible) on `localhost:9000` (console `localhost:9001`)

Wait until Postgres is healthy before starting the API.

---

## 3. Start the backend

```bash
cd apps/api
./mvnw quarkus:dev
```

API base: `http://localhost:8080`

Check:

```bash
curl -s http://localhost:8080/api/v1/health
curl -s http://localhost:8080/api/v1/status
```

---

## 4. Start the frontend

From the repository root (first time and when lockfile changes):

```bash
pnpm install
```

Then:

```bash
pnpm --filter web dev
```

Web: `http://localhost:3000`  
Status page: `http://localhost:3000/status`  
Login: `http://localhost:3000/masuk`  
Account: `http://localhost:3000/akun`

---

## 4b. Authentication locally

Google/Apple buttons appear only when provider secrets are set in `.env`.

Without those secrets, enable the development stub (never in production):

```bash
export AUTH_DEV_LOGIN=true
cd apps/api
./mvnw quarkus:dev
```

Then use **Masuk pengembangan** on `/masuk`. Bootstrap an administrator for local review APIs:

```bash
export AUTH_BOOTSTRAP_ADMINS=GOOGLE:admin-1
```

Sign in with provider `GOOGLE` and subject `admin-1`.

Redirect URIs to register at Google/Apple when using real OIDC:

```text
http://localhost:8080/api/v1/auth/google/callback
http://localhost:8080/api/v1/auth/apple/callback
```

---

## 5. Tests and quality checks

Backend tests (`./mvnw -B verify`) include `@QuarkusTest` integration tests that require PostgreSQL. Start Compose first.

From the repository root:

```bash
docker compose up -d
cd apps/api
./mvnw -B verify
```

Frontend (from repo root):

```bash
pnpm --filter web lint
pnpm --filter web test
pnpm --filter web build
pnpm --filter web format:check
```

### Browser E2E (Playwright, optional)

Requires API and web running with dev login (see [E2E_TESTING.md](E2E_TESTING.md)):

```bash
export AUTH_DEV_LOGIN=true
export AUTH_BOOTSTRAP_ADMINS=GOOGLE:admin-1
# start API + web as above, then:
pnpm --filter web exec playwright install chromium
pnpm test:e2e
```

Backend format check:

```bash
cd apps/api
./mvnw -B spotless:check
```

---

## 6. Stop infrastructure

```bash
docker compose down
```

Data is stored in a named Docker volume. Remove volumes only if you intend to wipe local Postgres/MinIO data:

```bash
docker compose down -v
```

---

## 7. Troubleshooting

- **API cannot reach Postgres / tests fail to boot:** start Compose first (`docker compose up -d`). Integration tests use the local Postgres instance on host port **55432**, not Testcontainers. If that port is busy, change the Compose mapping and `DATABASE_URL` together.
- **CORS errors in the browser:** `CORS_ORIGINS` must include the exact web origin (`http://localhost:3000`).
- **Frontend cannot reach API:** `NEXT_PUBLIC_API_URL` must be `http://localhost:8080` for local Next.js (browser calls the API directly).
- **Port 8080 or 3000 busy:** stop the other process or change `QUARKUS_HTTP_PORT` / `PORT`.
