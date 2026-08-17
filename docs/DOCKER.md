# Docker — Belajar Bersama local stack

Run the full MVP locally with Docker Compose: PostgreSQL, MinIO, API, Web, and Appium.

## Quick start

```bash
cp .env.docker.example .env.docker
docker compose up -d --build
```

Wait for health:

```bash
curl -s http://localhost:8080/api/v1/health
curl -s http://localhost:3000/status
curl -s http://localhost:4723/status
```

Open:

- Web: http://localhost:3000
- API: http://localhost:8080/api/v1/health
- MinIO console: http://localhost:9001 (minioadmin / minioadmin)
- Appium: http://localhost:4725/status (host port; container uses 4723)

Dev login is enabled in the Compose stack (`AUTH_DEV_LOGIN=true`, bootstrap admin `GOOGLE:admin-1`).

## Services

| Service | Port | Image |
|---|---|---|
| postgres | 55432 → 5432 | postgres:16-alpine |
| minio | 9000, 9001 | minio/minio |
| api | 8080 | `infra/docker/api/Dockerfile` |
| web | 3000 | `infra/docker/web/Dockerfile` |
| appium | 4725 → 4723 (default) | `infra/docker/appium/Dockerfile` |

Browser calls use **localhost** URLs (`NEXT_PUBLIC_API_URL`, CORS). Inside the Compose network, the API uses `postgres` and `minio` hostnames.

## Infrastructure only (host dev)

Run Postgres + MinIO without building apps:

```bash
docker compose up -d postgres minio minio-init
```

Then use `docs/DEVELOPMENT_SETUP.md` for `./mvnw quarkus:dev` and `pnpm --filter web dev`.

## Appium tests

See [testing/appium/README.md](../testing/appium/README.md).

```bash
cd testing/appium && npm install
npm run test:ping
npm run test:smoke
```

Optional Android emulator (heavy, profile `mobile`):

```bash
docker compose --profile mobile up -d android
```

If ports **8080**, **3000**, **9000**, or **4723** are already in use on the host, set overrides in `.env.docker`:

```bash
API_HOST_PORT=18080
WEB_HOST_PORT=13000
MINIO_API_PORT=19000
MINIO_CONSOLE_PORT=19001
APPIUM_HOST_PORT=4725
```

When using non-default API/web ports, rebuild the web image with matching `NEXT_PUBLIC_API_URL` and update `CORS_ORIGINS` on the API service.

## Stop

```bash
docker compose down
docker compose down -v   # wipe volumes
```
