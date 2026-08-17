# Review Model — Belajar Bersama

---

## Records

**ContentSubmission** — one review cycle for one revision. Status: `SUBMITTED | IN_REVIEW | CHANGES_REQUESTED | APPROVED`. At most one open (`SUBMITTED`/`IN_REVIEW`) submission per content item.

**ContentReview** — reviewer, submission, revision, decision (`APPROVE | REQUEST_CHANGES` or null while in progress), comment, timestamps.

The decision is tied to the exact `revisionId`. Approving does not modify content.

---

## Independence

`MakerCheckerPolicy` rejects `reviewer == maker` with `MAKER_CANNOT_REVIEW_OWN_CONTENT`.

Makers cannot start, approve, request-changes, or replace a checker because they dislike a decision. Administrative assign is an explicit, audited operation (`CONTENT_REVIEW_ASSIGNED`).

---

## After changes requested

The reviewed revision remains stored. The maker creates/edits a newer revision and resubmits. Previous reviews stay readable and immutable.
