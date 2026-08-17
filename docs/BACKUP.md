# Backup and recovery — Belajar Bersama

No automated production backup is implemented. Do not claim backup capability exists in operations until it is tested.

---

## Local / development

PostgreSQL data lives in the Compose volume. A manual dump is sufficient for a developer workstation:

```bash
docker compose exec postgres pg_dump -U belajar belajar_bersama > backup.sql
```

Restore only onto an empty or disposable database. Do not run unreviewed dumps against a database that already holds user data.

Object storage (MinIO) is local and disposable in development. Educational binaries are not the source of truth for curriculum text (that is PostgreSQL revisions).

Flyway migrations are the schema recovery path. Do not apply destructive migrations casually.

---

## Production (not chosen)

Hosting, managed Postgres, and backup retention are operational decisions with cost impact. Until hosting is selected:

- Treat `pg_dump` / provider automated backups as the intended PostgreSQL strategy
- Treat S3-compatible versioning or replication as the intended object-storage strategy
- Document restore tests before the first production cut (`docs/RELEASE.md`)
