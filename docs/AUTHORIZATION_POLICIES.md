# Authorization Policies — Belajar Bersama

Domain policies implemented in `AuthorizationPolicies` and `MakerCheckerPolicy`. Content create, review, and publish services call `assertCanCreateContent` / `assertCanReview` on every write. UI query endpoints are gating only.

Query endpoints exist so clients and tests can ask without creating content:

- `GET /api/v1/authorization/can-create-content?competencyId=`
- `GET /api/v1/authorization/can-review?competencyId=&makerId=`

Those queries return `{ "allowed": true|false }`. Writes call `assertCanCreateContent` / `assertCanReview`. Multi-competency overloads require an approved verification for **every** required competency.

---

## 1. `canCreateContent(user, competency)`

Allowed only when **all** are true:

```text
User.status == ACTIVE
AND permissions contain CONTENT_CREATE
    (from derived VERIFIED_CONTRIBUTOR)
AND there is an APPROVED verification for that competency
```

Not sufficient:

- authenticated learner
- approved verification in a **different** competency
- suspended/deactivated user
- `VERIFIED_CONTRIBUTOR` without matching competency (the derived role appears if **any** competency is approved, but create is still scoped)

---

## 2. `canReview(user, competency, makerId)`

Allowed only when **all** are true:

```text
User.status == ACTIVE
AND stored roles contain CHECKER
AND there is an APPROVED verification for that competency
AND makerId != user.id
```

Resolved open decision **#7**: checker eligibility is **CHECKER role + approved verification for the competency + not the maker**. There is no separate `CheckerEligibility` table in this phase.

A checker without verification for Java cannot review Java content even if they can review Mathematics.

---

## 3. Maker ≠ Checker

`MakerCheckerPolicy.assertCheckerIsNotMaker(makerId, checkerId)` throws `MAKER_CANNOT_REVIEW_OWN_CONTENT` when the ids are equal.

`canReview` returns false in that case; `assertCanReview` throws. The frontend must not be the enforcement point. Content review services pass the submission maker id into this policy.

---

## 4. Inactive users

`assertActive` throws `USER_NOT_ACTIVE` for null, `SUSPENDED`, or `DEACTIVATED` users.

Session resolution also refuses inactive users, so protected APIs appear unauthenticated (`401`) after suspend/deactivate. Sessions are revoked at the same time.

---

## 5. Content workflow

These policies do not replace the content state machine. They only answer **who** may create or review. Transitions live in `ContentLifecycle` and the content/review services.
