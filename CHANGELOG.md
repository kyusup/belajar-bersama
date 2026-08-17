# Changelog — Belajar Bersama

All notable implementation work is recorded here. Dates are development timestamps, not production releases.

## Unreleased

### Phase 11 — Docker full stack and Appium tests

- Docker Compose runs Postgres, MinIO, API, Web, and Appium locally
- Dockerfiles under `infra/docker/{api,web,appium}`
- Next.js `output: 'standalone'` for container image
- `testing/appium` — WebdriverIO + Appium mobile-web smoke tests
- [docs/DOCKER.md](docs/DOCKER.md)

### Phase 10 — Git repository

- Initialize GitHub remote `git@github.com:kyusup/belajar-bersama.git`
- Phase commits (0–2 through 9) pushed to `main`
- `scripts/git-phase-commits.sh` for reproducible phase history

### Phase 9 — MVP audit and release readiness

- Constitution compliance audit ([MVP_AUDIT.md](docs/MVP_AUDIT.md))
- Refreshed living architecture/setup/release docs
- Security tests: dev login disabled, admin permission boundaries, rate-limit `429`
- Updated consistency checklist for Phases 3–8

### Phase 8 — E2E and release readiness

- API golden-path journey test (`CriticalJourneyResourceTest`)
- Playwright browser E2E (smoke, auth, learning) with global fixture seeding
- [docs/E2E_TESTING.md](docs/E2E_TESTING.md)

### Phase 7 — Security hardening

- In-memory API rate limits (auth/write/report/search/public)
- Origin/Referer allow-list on mutating requests
- Admin user directory by display name (no email)
- Web CSP and Permissions-Policy headers

### Phase 6 — Q&A and moderation

- Public learning Q&A (`/tanya`) readable without authentication
- Authenticated ask/answer/close/accept/useful/report
- Hybrid accepted-answer rule: asker of that question, or moderator (`CONTENT_MODERATE`)
- Hide-not-delete moderation queue (`/moderasi`) for Q&A and educational content reports
- Public search includes non-hidden Q&A (`type=QA_QUESTION`)
- Administrator console (`/kelola`) for verification review, role assignment, and taxonomy
- Learners can apply for competency verification from `/akun`

### Phase 5 — Learning experience

- Browse published paths/courses/lessons/materials/quizzes
- Explicit lesson completion, bookmarks, resume, computed progress
- Server-authoritative quiz scoring; attempts immutable and revision-pinned

### Phase 4 — Educational content

- Taxonomy, revisions, maker–checker, publication, content reports

### Phase 3 — Identity

- Google/Apple OIDC BFF, session cookie, RBAC, competency-scoped verification

### Phase 0–2 — Foundation

- Product constitution, domain model, architecture, CI, Docker Compose local stack
