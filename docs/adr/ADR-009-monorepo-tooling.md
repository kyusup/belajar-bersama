# ADR-009: Monorepo Tooling

## Status

Accepted

Resolves former `OPEN_DECISIONS.md` item 21.

## Context

The repo contains Next.js and Quarkus. Tooling needed to be chosen beyond empty folders.

## Decision

- **pnpm workspaces** for JavaScript/TypeScript (`apps/web`, `packages/shared`)
- **Maven** single module for Quarkus, with **package-layer** modularity (not a Maven multi-module reactor yet)
- Maven Wrapper committed under `apps/api`

## Alternatives

- npm/yarn workspaces: acceptable; pnpm is already present and stricter with hoisting
- Quarkus Maven multi-module (`domain` jar, `app` jar): more isolation, more Phase 2 ceremony
- Nx/Turborepo: extra abstraction before it is needed

## Consequences

- `pnpm install` from repo root
- Java remains independently buildable with `./mvnw`
- A future Maven multi-module split is possible if domain reuse requires it
