# ADR-001: Frontend Architecture

## Status

Accepted

## Context

Belajar Bersama needs a public web UI in Indonesian, with anonymous access to published learning and later authenticated contribution flows. The Product Constitution forbids putting authorization truth in the client.

## Decision

Use **Next.js (App Router) + TypeScript** in `apps/web`.

Separate:

- `src/app` — routes and presentation
- `src/components` — shared UI
- `src/lib/api` — API communication
- `src/lib/auth` — authentication state (placeholder in Phase 2)
- `packages/shared` — domain-facing TypeScript types

Do not encode verification, Maker–Checker, or publication rules in React components.

## Alternatives

- SPA (Vite + React only): weaker document/routing defaults for a content-heavy public site
- Server-driven Java UI: mismatches the chosen Next.js direction
- Putting business rules in React: violates backend-as-authority

## Consequences

- SEO and public learning pages can be rendered without login
- Requires disciplined API-client boundaries
- Auth implementation (later) must not leak tokens into business components
