# ADR-008: Audit Strategy

## Status

Accepted

## Context

Verification, review, publish, role changes, and reports must be reconstructible. PII and secrets must not be dumped into logs.

## Decision

- Domain port `AuditRecorder`
- Persistence: append-only `audit_event` rows (UUID, actor, action, target, timestamp, correlation id, JSON metadata)
- Emit from application services on successful state changes (later phases)
- Allow-list metadata keys; never store tokens, passwords, or emails unless a future legal requirement says so (default: no)

Phase 2 creates the table, port, and PostgreSQL adapter. Product events are not yet produced by real workflows.

## Alternatives

- Only application log files: hard to query, easy to leak PII, weak integrity
- Audit columns on every table: noisy and poor for “who approved this”
- External SIEM only: not the system of record for governance

## Consequences

- Schema stays small until workflows exist
- Correlation IDs join HTTP logs with audit rows
