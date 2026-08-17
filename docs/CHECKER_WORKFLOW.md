# Checker Workflow — Belajar Bersama

Independent review of submitted revisions.

---

## Eligibility

```text
User ACTIVE
AND stored role CHECKER
AND APPROVED verification for every competency on the revision
AND checker.id != maker.id
```

---

## Assignment (MVP hybrid)

- Administrator may assign (`POST /api/v1/admin/content/{id}/assign-reviewer`) using `SYSTEM_ADMIN`. Only eligible checkers can be assigned.
- Eligible checkers may claim a `SUBMITTED` item (`POST /api/v1/reviews/{submissionId}/start`).
- Concurrent start uses optimistic locking (`REVIEW_ALREADY_ACTIVE`).
- The maker cannot assign or choose the checker.

---

## Review operations

Queue: `GET /api/v1/reviews/my` (eligible `SUBMITTED` plus assigned `IN_REVIEW`).

```text
POST /api/v1/reviews/{submissionId}/start
POST /api/v1/reviews/{submissionId}/approve
POST /api/v1/reviews/{submissionId}/request-changes
```

The checker cannot edit the maker’s body. Comments are overall notes (inline block comments are a later extension).

UI: `/tinjauan`, `/tinjauan/{submissionId}`. Maker UI does not expose these actions.
