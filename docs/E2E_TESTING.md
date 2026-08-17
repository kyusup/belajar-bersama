# E2E Testing — Belajar Bersama

Two layers cover critical user journeys:

| Layer | Location | Runs in CI | Requires |
|---|---|---|---|
| API journey | `CriticalJourneyResourceTest` | Yes (with Postgres service) | Docker Compose or CI Postgres |
| Browser E2E | `apps/web/e2e` (Playwright) | No (local/staging) | API + web + Postgres running |

---

## API golden path (CI)

`CriticalJourneyResourceTest` exercises one end-to-end REST journey:

1. Verify contributor + checker
2. Maker–checker publish material
3. Anonymous public read + search
4. Learner bookmark
5. Q&A ask → answer → accept
6. Content report → moderator resolve

Run with other backend tests:

```bash
docker compose up -d
cd apps/api
./mvnw -B verify
```

---

## Browser E2E (Playwright)

### Prerequisites

1. Postgres (Compose): `docker compose up -d`
2. API with dev auth enabled:

```bash
export AUTH_DEV_LOGIN=true
export AUTH_BOOTSTRAP_ADMINS=GOOGLE:admin-1
cd apps/api
./mvnw quarkus:dev
```

3. Web (separate terminal):

```bash
pnpm --filter web dev
```

Optional overrides:

- `E2E_BASE_URL` — default `http://localhost:3000`
- `E2E_API_URL` — default `http://localhost:8080`

### Install browsers (first time)

```bash
pnpm --filter web exec playwright install chromium
```

### Run

```bash
pnpm test:e2e
```

Global setup waits for API/web health, then seeds one published material via the REST API (maker–checker workflow). Specs cover:

- **smoke** — home, status, subjects
- **auth** — dev login → account page
- **learning** — read published fixture, bookmark, ask Q&A

Interactive mode:

```bash
pnpm --filter web test:e2e:ui
```

Reports: `apps/web/playwright-report/` (open with `pnpm --filter web exec playwright show-report`).

---

## Why browser E2E is not in CI yet

Starting Quarkus + Next.js + Postgres in GitHub Actions adds minutes and flakiness. The API journey test guards business invariants in CI; Playwright validates the Indonesian UI wiring locally before release candidates.

See also [RELEASE.md](RELEASE.md) for the pre-production checklist.
