# Maker–Checker — Belajar Bersama

Maker–Checker is a first-class content review domain concept, not only a human checklist.

---

## 1. Purpose

Ensure educational content is reviewed by an eligible checker before publication, with:

- clear submission/revision identity
- recorded decisions and comments
- audit history
- separation of maker and approver

---

## 2. Minimum Lifecycle

```text
DRAFT
SUBMITTED
IN_REVIEW
CHANGES_REQUESTED
RESUBMITTED
APPROVED
PUBLISHED
ARCHIVED
```

Allowed transitions and side effects are detailed in `CONTENT_LIFECYCLE.md`. This document defines review intent and hard rules.

---

## 3. Submission Must Capture

A content submission/revision package must be able to represent:

| Element | Meaning |
|---|---|
| Maker | User who authors/submits |
| Content | Educational artifact under review |
| Submission / revision | Immutable-enough revision identity for audit |
| Checker / reviewer | Assigned or acting eligible checker when present |
| Review decision | Approve, request changes, or other recorded outcome |
| Review comments | Feedback to maker |
| Timestamps | Created/submitted/reviewed/published/etc. |
| Audit history | Who did what, when |

---

## 4. Hard Rules

1. **No self-approval** — Maker must never approve their own submission.
2. **Checker eligibility** — Checker must be eligible for the relevant competency/domain.
3. **Feedback over silent overwrite** — Normal path is request-changes → maker revises → resubmit. Checker silently rewriting maker work is not the default workflow.
4. **Publication gate** — Educational content reaches `PUBLISHED` only through allowed review outcomes and transitions.
5. **Auditability** — Review decisions and key transitions emit audit records.

---

## 5. Roles in a Single Submission

| Actor | Allowed on that submission |
|---|---|
| Maker | Create, edit draft, submit, revise after changes requested |
| Checker | Review if eligible and not the maker; decide approve / request changes |
| Moderator | Handle reports/policy issues; not a substitute quality approver by default |
| Administrator | Governance operations; force-publish authority is `OPEN DECISION` |

---

## 6. Competency Alignment

- Maker must be verified for the competency scope of the content being submitted.
- Checker must be eligible for that competency/domain.
- Exact mapping between “verified maker competency” and “checker eligibility”: largely aligned by competency, with remaining edge cases in `OPEN_DECISIONS.md` if needed.

---

## 7. Revision Principle

Each meaningful resubmission should be traceable as a revision so reviewers and auditors can see what changed and what was decided.

Whether prior approved revisions remain readable after archive/replace: `OPEN DECISION` for product UX; domain should retain history.
