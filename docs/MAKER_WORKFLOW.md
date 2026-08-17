# Maker Workflow — Belajar Bersama

Verified contributors create and submit educational content. Enforcement is server-side (`AuthorizationPolicies.assertCanCreateContent`).

---

## Eligibility

```text
User ACTIVE
AND authenticated
AND CONTENT_CREATE (derived VERIFIED_CONTRIBUTOR)
AND APPROVED verification for every competency on the revision
```

A Java-verified contributor cannot create Mathematics content.

Suspended/deactivated users cannot act (session appears unauthenticated).

---

## Operations

1. Create draft (`POST /api/v1/content`) — taxonomy and at least one competency required.
2. Edit own draft (`PATCH /api/v1/content/{id}`) — never a status field.
3. Submit (`POST /api/v1/content/{id}/submit`) — title, non-empty sanitized body, subject, level, competency, license.
4. After `CHANGES_REQUESTED`, edit (new revision) and submit again.
5. After `APPROVED`, publish (`POST /api/v1/content/{id}/publish`).
6. Archive (`POST /api/v1/content/{id}/archive`) with `CONTENT_ARCHIVE`.

UI: `/konten-saya`, `/konten-saya/baru`, `/konten-saya/{id}`.

---

## Forbidden

- Approve or review own content (`MAKER_CANNOT_REVIEW_OWN_CONTENT`)
- Publish without `APPROVED`
- Choose their own checker
- Edit another maker’s content (treated as not found)
- Mutate audit history or published revisions in place
