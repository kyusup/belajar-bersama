# Verification Model — Belajar Bersama

Competency-scoped verification as implemented in Phase 3. Product rules: `VERIFICATION.md`.

There is **no** `user.is_verified` flag.

---

## 1. Shape

```text
User
  └── Verification (per competency)
        ├── Competency (managed data)
        ├── Status
        ├── Qualification / experience
        ├── Evidence metadata
        ├── Reviewer
        ├── Decision note
        └── Audit history (audit_event rows)
```

---

## 2. Competency

Table `competency` is data, not a Java enum. Seeded examples:

| Name | Slug |
|---|---|
| Mathematics | `matematika` |
| Statistics | `statistika` |
| Java | `java` |
| Backend Development | `backend-development` |
| Accounting | `akuntansi` |
| Physics | `fisika` |
| English Language | `bahasa-inggris` |

`GET /api/v1/competencies` is anonymous. Administrators will manage the catalog in a later phase (`TAXONOMY_MANAGE`).

---

## 3. States

```text
DRAFT
SUBMITTED
UNDER_REVIEW
CHANGES_REQUESTED
APPROVED
REJECTED
REVOKED
```

Applicant flow in this phase: `POST /api/v1/verifications` creates or resubmits as `SUBMITTED`. Resubmit is allowed from `DRAFT` or `CHANGES_REQUESTED` for the same competency.

Unique indexes:

- at most one `APPROVED` row per `(user, competency)`
- at most one open row (`DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `CHANGES_REQUESTED`) per `(user, competency)`

---

## 4. Evidence

`verification_evidence` stores structured metadata:

- `kind` (for example `education`)
- `summary` (required for a row to be stored)
- optional `reference_url`
- optional `storage_key` (no upload pipeline in this phase)

---

## 5. Applicant APIs

| Method | Path | Who |
|---|---|---|
| POST | `/api/v1/verifications` | Authenticated, `VERIFICATION_APPLY`, `ACTIVE` |
| GET | `/api/v1/verifications/me` | Same user |

Body: competency, qualification (required), experience, optional evidence list.

---

## 6. Review APIs

Only `ADMINISTRATOR` permissions in this phase (not checkers).

| Method | Path |
|---|---|
| GET | `/api/v1/admin/verifications` |
| GET | `/api/v1/admin/verifications/{id}` |
| POST | `/api/v1/admin/verifications/{id}/start-review` |
| POST | `/api/v1/admin/verifications/{id}/approve` |
| POST | `/api/v1/admin/verifications/{id}/reject` |
| POST | `/api/v1/admin/verifications/{id}/request-changes` |
| POST | `/api/v1/admin/verifications/{id}/revoke` |

List pending currently returns `SUBMITTED` applications.

---

## 7. Invariants

1. A user cannot verify themselves (`applicantId == reviewerId` is rejected even if the actor is an administrator reviewing their own application).
2. `REJECTED` does not grant contributor eligibility.
3. `REVOKED` removes contributor eligibility for that competency.
4. Verification is competency-scoped. Mathematics approval does not grant Java.
5. Approval stores `reviewer_id`.
6. Every transition writes `audit_event`.
7. Suspended/deactivated users cannot submit or review.

Self-approval of the admin API is also blocked by missing `VERIFICATION_APPROVE` on learners.
