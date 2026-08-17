# ADR-006: Search Strategy

## Status

Accepted

## Context

Learners will search subjects, paths, and lessons. There is no current requirement for a dedicated search cluster.

## Decision

Define domain port `SearchIndex`.

- Initial adapter: **PostgreSQL** (full-text search when content exists)
- Do not introduce OpenSearch/Elasticsearch in this phase
- Keep the port small (`index`, `remove`, `search`) so a later engine can be added

Phase 2 adapter is a stub that is wired and tested, without indexing educational content.

## Alternatives

- OpenSearch now: operational cost without a proven need
- Client-only filtering: cannot scale and bypasses access rules

## Consequences

- Search relevance work is deferred
- Access control must be applied when search returns hits (published vs draft)
