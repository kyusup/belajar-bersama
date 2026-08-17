# ADR-007: Authorization Strategy

## Status

Accepted (model); resource enforcement not applied in Phase 2

## Context

Roles stack. Verification is competency-scoped. Maker and Checker must stay separated per submission. Scattered `isAdmin` checks will miss invariants.

## Decision

- Explicit **permissions** (`CONTENT_SUBMIT`, `CONTENT_APPROVE`, …) documented in `PERMISSION_MODEL.md`
- Application services check permissions **and** domain policies (`MakerCheckerPolicy`, verification scope)
- Frontend permission hiding is optional UX only

## Alternatives

- Role checks only (`if (CHECKER)`): insufficient for competency and self-approval
- Frontend-only guards: trivial to bypass
- Global `user.isVerified`: forbidden by `VERIFICATION.md`

## Consequences

- Permission catalog can evolve without renaming roles
- Open decisions (force-publish, archive authority, checker eligibility storage) still constrain some grants
