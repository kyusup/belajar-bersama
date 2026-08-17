# ADR-010: API Contract Style

## Status

Accepted

Resolves former `OPEN_DECISIONS.md` item 22.

## Context

Web and API need a stable contract. Generating clients too early can freeze a skeleton API. Hand-waving REST without OpenAPI is harder to test.

## Decision

- **REST** as the public API style
- **Code-first OpenAPI** via Quarkus SmallRye (`/q/openapi`)
- Shared TypeScript types in `packages/shared` maintained by hand for the small Phase 2 surface
- Do not require committed codegen in CI yet

## Alternatives

- OpenAPI-first with committed spec and generated servers: heavier for a health-only skeleton
- GraphQL: extra complexity; public learning + command-style review actions fit REST well
- gRPC-web: poor browser/public-API fit for this product

## Consequences

- Documented error envelope is the compatibility surface
- Later, codegen can be added without changing URL conventions
