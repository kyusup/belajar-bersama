# Docker image definitions

| Dockerfile | Service | Notes |
|---|---|---|
| `api/Dockerfile` | Quarkus API (JVM fast-jar) | Build context: repository root |
| `web/Dockerfile` | Next.js standalone | `output: 'standalone'` in `next.config.ts` |
| `appium/Dockerfile` | Appium 2 + Chromium driver | Mobile-web tests via Compose |

Full local stack: [docs/DOCKER.md](../../docs/DOCKER.md).

Phase 2+ also supports running Postgres/MinIO in Compose while building apps on the host (`docs/DEVELOPMENT_SETUP.md`).
