# Content Lifecycle — Belajar Bersama

Implemented state machine for educational content. Aligns with `MAKER_CHECKER.md`.

`RESUBMITTED` from Phase 1 is **not** a separate status. Resubmit returns to `SUBMITTED` with a new revision number.

---

## 1. States

```text
DRAFT → SUBMITTED → IN_REVIEW → CHANGES_REQUESTED | APPROVED → PUBLISHED → ARCHIVED
```

| State | Meaning |
|---|---|
| DRAFT | Maker is editing the current revision |
| SUBMITTED | Awaiting checker claim or admin assignment |
| IN_REVIEW | An eligible checker has started review |
| CHANGES_REQUESTED | Checker requested corrections; reviewed revision stays immutable |
| APPROVED | Checker approved **this** revision; not yet public |
| PUBLISHED | `publishedRevisionId` points at the approved revision |
| ARCHIVED | Soft-archived; removed from public discovery |

---

## 2. Allowed transitions (`ContentLifecycle`)

```text
DRAFT → SUBMITTED | ARCHIVED
SUBMITTED → IN_REVIEW | ARCHIVED
IN_REVIEW → CHANGES_REQUESTED | APPROVED | ARCHIVED
CHANGES_REQUESTED → SUBMITTED | DRAFT | ARCHIVED
APPROVED → PUBLISHED | ARCHIVED
PUBLISHED → DRAFT | ARCHIVED
ARCHIVED → (none)
```

`PUBLISHED → DRAFT` is the “edit published content” path: a **new** revision is created; `publishedRevisionId` is unchanged until a later publish.

There is no generic `PATCH` of `status`. Clients call domain operations: submit, start, approve, request-changes, publish, archive.

---

## 3. Publication

Approval does **not** make content public (open decision #3 resolved for this phase).

The maker (or an administrator with `CONTENT_PUBLISH`) calls publish while status is `APPROVED`. Publish sets `publishedRevisionId = currentRevisionId` and indexes search.

Anonymous readers see only that published revision.

---

## 4. Archive

Soft archive (`archived_at`). Maker or administrator with `CONTENT_ARCHIVE`. Search document is removed. Revision and audit history remain. Physical delete is out of scope.
