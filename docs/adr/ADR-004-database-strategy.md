# ADR-004: Database Strategy

## Status

Accepted

## Context

The domain model is richer than screens. Schema must evolve without becoming the source of business rules.

## Decision

- **PostgreSQL 16** as the only transactional system of record for MVP
- **UUID** primary keys
- **Flyway** migrations in `apps/api/src/main/resources/db/migration`
- `created_at` / `updated_at` on mutable entities when those tables are introduced
- Optimistic concurrency (`version`) on aggregates that see concurrent edits (content drafts, later)
- Dedicated `audit_event` table rather than copy-on-write audit columns on every table
- Foreign keys and unique constraints for structural integrity; **domain layer still enforces** Maker–Checker and verification rules

Phase 2 ships only foundation migrations (audit table + extensions), not the full content model.

## Alternatives

- Schema-per-tenant: no multi-tenancy requirement now
- NoSQL as primary store: poor fit for relational governance workflows
- Hibernate `ddl-auto` in production: unsafe as source of schema

## Consequences

- Migrations are reviewable in PRs
- Incomplete domain tables are intentional until Phase 3+
- Application must not assume UI-shaped tables
