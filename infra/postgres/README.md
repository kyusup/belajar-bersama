# PostgreSQL notes

Local database is started by the root `docker-compose.yml`.

- Engine: PostgreSQL 16
- Host port: `55432` (container `5432`)
- Database: `belajar_bersama`
- Migrations: Flyway in `apps/api/src/main/resources/db/migration`

Do not add ad-hoc SQL that bypasses Flyway.
