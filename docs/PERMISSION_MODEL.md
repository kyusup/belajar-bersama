# Permission Model — Belajar Bersama

Conceptual permission catalog. The implemented Role → Permission contract is `PERMISSION_MATRIX.md`. Runtime enforcement is `RolePermissionCatalog`.

Roles remain those in `GOVERNANCE.md` and `ROLE_PERMISSION_CONCEPT.md`. Permissions are the explicit capabilities those roles may grant. Domain invariants still apply even when a permission is present.

---

## 1. Rules

1. Authorize by permission + domain policy, not by `if (isAdmin)` scattered in handlers.
2. A user may hold multiple roles; permissions are the union, then **invariants subtract** (maker cannot approve own submission).
3. Verification is **not** a boolean permission; it is competency-scoped (`VERIFICATION.md`).
4. Frontend may mirror permission names for UX later; the API is authoritative.

---

## 2. Permission catalog

| Permission | Meaning | Typical grant |
|---|---|---|
| `CONTENT_READ_PUBLISHED` | Read published educational content | Anonymous and all users |
| `LEARNING_PROGRESS_MANAGE` | Own progress records | Authenticated learner |
| `BOOKMARK_MANAGE` | Own bookmarks | Authenticated learner |
| `QUIZ_HISTORY_READ` | Own quiz history | Authenticated learner |
| `QA_ASK` | Create a Q&A question | Authenticated |
| `QA_ANSWER` | Answer a Q&A question | Authenticated |
| `QA_MARK_USEFUL` | Mark an answer useful | Authenticated |
| `QA_ACCEPT_ANSWER` | Catalog only; unused grant. Accept is asker or `CONTENT_MODERATE` | Not assigned |
| `CONTENT_REPORT` | Report content/Q&A | Authenticated; anonymous reporting is `OPEN DECISION` |
| `CONTENT_CREATE` | Create educational drafts | Maker with active verification for competency |
| `CONTENT_UPDATE_DRAFT` | Edit own draft/revision | Maker (owner) |
| `CONTENT_SUBMIT` | Submit/resubmit for review | Maker (owner, verified scope) |
| `CONTENT_REVIEW` | Start/perform review | Eligible checker, not maker |
| `CONTENT_REQUEST_CHANGES` | Request changes | Eligible checker, not maker |
| `CONTENT_APPROVE` | Approve submission | Eligible checker, not maker |
| `CONTENT_PUBLISH` | Publish approved content | Per lifecycle; admin force-publish is `OPEN DECISION` |
| `CONTENT_ARCHIVE` | Archive content | `OPEN DECISION` who |
| `CONTENT_MODERATE` | Moderation actions on reports/content | Moderator / Admin |
| `VERIFICATION_GRANT` | Grant competency verification | Administrator |
| `VERIFICATION_REVOKE` | Revoke/suspend verification | Administrator |
| `TAXONOMY_MANAGE` | Maintain subjects/levels/competencies | Administrator |
| `ROLE_ASSIGN` | Assign/remove roles | Administrator |
| `AUDIT_READ` | Read audit events | Administrator (narrower grants later) |

Names may gain resource-specific suffixes later (`CONTENT_APPROVE` remains the canonical verb).

---

## 3. Invariants that permissions cannot override

- `MAKER_CANNOT_REVIEW_OWN_CONTENT` / cannot approve own submission
- `USER_NOT_VERIFIED_FOR_COMPETENCY` for create/submit outside scope
- `INVALID_CONTENT_TRANSITION` for illegal lifecycle moves
- Public emails and identity provider subjects are never authorized for public projection

---

## 4. Mapping note

Maker capability is **Verification(competency) → CONTENT_CREATE/SUBMIT**, not a global `isVerified` flag.

Checker capability is **Checker role + approved verification for that competency + not maker** (`AUTHORIZATION_POLICIES.md`). Open decision #7 is resolved.
