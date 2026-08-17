# Belajar Bersama

Open learning platform and knowledge community for Indonesian society.

Public published lessons and non-hidden Q&A can be read without an account. Contributors need competency-scoped verification. Independent checkers review educational content before publication.

**Status:** Local MVP complete (Phases 0–8). Constitution audit passed — see [docs/MVP_AUDIT.md](docs/MVP_AUDIT.md). Not a production deploy.

## Quick start

```bash
cp .env.example .env
docker compose up -d
cd apps/api && ./mvnw quarkus:dev
# in another terminal, from repo root:
pnpm install
pnpm --filter web dev
```

- Web: http://localhost:3000
- Q&A: http://localhost:3000/tanya
- Login: http://localhost:3000/masuk
- API health: http://localhost:8080/api/v1/health

Full commands: [docs/DEVELOPMENT_SETUP.md](docs/DEVELOPMENT_SETUP.md)

## Documentation

- [Product Constitution](docs/PRODUCT_CONSTITUTION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Domain Model](docs/DOMAIN_MODEL.md)
- [Q&A](docs/QA_MODEL.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [MVP audit](docs/MVP_AUDIT.md)
- [Release bar](docs/RELEASE.md)
- [Changelog](CHANGELOG.md)
- [E2E testing](docs/E2E_TESTING.md)
- [Open Decisions](docs/OPEN_DECISIONS.md)

Full index: [docs/README.md](docs/README.md)

## Stack

- **Web:** Next.js + TypeScript (`apps/web`)
- **API:** Java 21 + Quarkus (`apps/api`)
- **Database:** PostgreSQL 16
- **Object storage (local):** MinIO (S3-compatible)
- **CI:** GitHub Actions

## Licensing (separation)

- **Source code:** Apache License 2.0
- **Educational content (recommended where applicable):** CC BY-SA

Software licensing and educational-content licensing are separate. Third-party materials are not assumed relicensable.
