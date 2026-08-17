# ADR-002: Backend Architecture

## Status

Accepted

## Context

The domain includes verification, Maker–Checker, publication, and audit invariants that must be enforced in software. Phase 1 assumed a Quarkus modular monolith.

## Decision

Use **Java 21 + Quarkus** as a **modular monolith** in `apps/api`, with package layers:

- `domain` — entities, value objects, policies, ports, domain errors
- `application` — use cases / application services
- `interfaces` — HTTP adapters
- `infrastructure` — PostgreSQL, S3, search adapters

The domain module must not depend on infrastructure types.

## Alternatives

- Multiple microservices now: operational cost with one team and no scale requirement
- Anemic “utils/services everywhere”: scatters invariants
- Spring Boot: viable, but contradicts the established Quarkus direction

## Consequences

- Single deployable for MVP
- Clear places for lifecycle and authorization tests
- Later split into services is possible at package/bounded-context seams
