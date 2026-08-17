# Content Revisioning — Belajar Bersama

How versions relate to review and public reads.

---

## 1. Identity vs snapshot

`EducationalContent` is the stable identity (id, slug, maker).

`ContentRevision` is a numbered snapshot. Review and publication always name a **revision id**.

---

## 2. When a new revision is created

| Situation | Behavior |
|---|---|
| Status `DRAFT` | Update the current revision in place |
| Status `CHANGES_REQUESTED` and current revision was already submitted | Create revision N+1; reviewed N stays immutable |
| Further edits after that new revision, still `CHANGES_REQUESTED` | Update the new revision in place |
| Status `PUBLISHED` | Create N+1 as `DRAFT`; public still reads published N |
| Status `SUBMITTED` / `IN_REVIEW` / `APPROVED` | Edits rejected (`CONTENT_NOT_EDITABLE`) |

---

## 3. What was approved?

A `ContentReview` stores `revisionId`. Approval does not copy or alter the body. A later draft cannot ride on a previous approval. Resubmission creates a new `ContentSubmission` for the new revision.

---

## 4. Slugs

Slugs are generated from the title at create time, URL-safe, unique (including `content_slug_history`). Title edits do **not** change the slug. Historical slugs resolve to the same content.
