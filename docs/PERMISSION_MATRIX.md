# Permission Matrix — Belajar Bersama

Explicit Role → Permission contract. Implemented by `RolePermissionCatalog`.

Permissions are **not** granted to every role. A check mark means the role contributes that permission to the user’s union. Domain policies may still deny the action.

`VERIFIED_CONTRIBUTOR` is derived from approved verification (see `RBAC.md`).

Anonymous callers have no session and receive no permissions. Public published learning is readable without authentication (`/api/v1/public/*` and the public web pages).

---

## LEARNER

Assigned on first login.

- ✓ `USER_READ_SELF`
- ✓ `CONTENT_READ_PUBLISHED`
- ✓ `LEARNING_PROGRESS_MANAGE`
- ✓ `BOOKMARK_MANAGE`
- ✓ `QUIZ_HISTORY_READ`
- ✓ `QA_CREATE`
- ✓ `QA_ASK`
- ✓ `QA_ANSWER`
- ✓ `QA_MARK_USEFUL`
- ✓ `CONTENT_REPORT`
- ✓ `VERIFICATION_APPLY`

Not granted: content authoring, review, moderation, verification decisions, taxonomy, user/role administration.

---

## VERIFIED_CONTRIBUTOR

Derived. Does not replace `LEARNER`.

- ✓ `CONTENT_CREATE`
- ✓ `CONTENT_EDIT_OWN`
- ✓ `CONTENT_UPDATE_DRAFT`
- ✓ `CONTENT_SUBMIT`
- ✓ `CONTENT_PUBLISH` (only after checker `APPROVED`; explicit publish)
- ✓ `CONTENT_ARCHIVE` (own content)

Creating content for a competency still requires an `APPROVED` verification for **that** competency (`AUTHORIZATION_POLICIES.md`).

---

## CHECKER

Manually assigned. Does not imply contributor rights.

- ✓ `CONTENT_REVIEW`
- ✓ `CONTENT_APPROVE`
- ✓ `CONTENT_REQUEST_CHANGES`

Reviewing a submission still requires an `APPROVED` verification for the relevant competency and `makerId ≠ checkerId`.

---

## MODERATOR

Manually assigned.

- ✓ `CONTENT_MODERATE`
- ✓ `CONTENT_REPORT_REVIEW`

---

## ADMINISTRATOR

Manually assigned or bootstrapped. Does **not** automatically receive contributor or checker content permissions. An administrator who should also check content must be assigned `CHECKER` and hold the relevant verification.

- ✓ `VERIFICATION_REVIEW`
- ✓ `VERIFICATION_APPROVE`
- ✓ `VERIFICATION_REVOKE`
- ✓ `VERIFICATION_GRANT`
- ✓ `TAXONOMY_MANAGE`
- ✓ `USER_MANAGE`
- ✓ `ROLE_MANAGE`
- ✓ `ROLE_ASSIGN`
- ✓ `SYSTEM_ADMIN`
- ✓ `AUDIT_READ`
- ✓ `CONTENT_ARCHIVE`
- ✓ `CONTENT_PUBLISH`

Administrators still do **not** receive `CONTENT_APPROVE`. Force-publish of unapproved content remains open decision #1.

---

## Catalog permissions not granted to any role yet

- `QA_ACCEPT_ANSWER` (catalog only; not granted. Accept is asker-or-moderator ownership, see `QA_MODEL.md`)
