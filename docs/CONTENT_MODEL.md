# Content Model — Belajar Bersama

Phase 4 educational content model as implemented.

Related: `CONTENT_LIFECYCLE.md`, `CONTENT_REVISIONING.md`, `CONTENT_API.md`.

This is **not** a general-purpose CMS. The model separates taxonomy, learning structure, and publishable content, and keeps reviewability ahead of editor sophistication.

---

## 1. Three layers

| Layer | Entities | Purpose |
|---|---|---|
| Taxonomy | `Subject`, `EducationLevel` | Governed classification. Administrators manage rows. Contributors **select** existing values. |
| Structure | `EducationalContent.kind` + optional `parent_id` | Learning path / course / module / lesson nesting when needed. |
| Content | `EducationalContent` + `ContentRevision` | The artifact that is drafted, reviewed, and published. |

A standalone material may exist with `kind = MATERIAL` and `parent_id = null`. Not every resource must belong to a course.

Competency is **not** taxonomy. Competencies come from Phase 3 and gate maker/checker eligibility.

---

## 2. Kinds

`LEARNING_PATH | COURSE | MODULE | LESSON | MATERIAL | QUIZ`

Structural parent-child is a single optional `parent_id` (tree, not many-to-many). Open decision #18 remains if a graph is needed later.

---

## 3. EducationalContent (identity)

Stable id and slug across revisions.

| Field | Role |
|---|---|
| `makerId` | Creator / Maker. Checkers never become authors by reviewing. |
| `subjectId` / `educationLevelId` | Required governed taxonomy |
| `status` | Workflow state of the **current** revision |
| `currentRevisionId` | Latest working revision (may be unpublished) |
| `publishedRevisionId` | Revision anonymous readers see, if any |
| `archivedAt` | Soft archive; history retained |
| `version` | Optimistic lock |

Public visibility = `publishedRevisionId IS NOT NULL AND archivedAt IS NULL`. A later draft does **not** hide the published revision.

---

## 4. ContentRevision

Immutable once it has entered review. Draft revisions may be updated in place until submit.

Each revision stores: title, summary, structured body, license, change summary, author (`createdBy`, preserved as the maker), timestamp, revision number, competency ids, sources.

Eligibility is evaluated against **the revision’s competencies**, not a global `user.isVerified` flag.

Multiple competencies: the maker (and checker) must have an `APPROVED` verification for **every** required competency.

---

## 5. Body format

JSON `{ "blocks": [ { "type", "level", "text", "ordered", "items", "language", "href" } ] }`.

Allowed types: `heading`, `paragraph`, `list`, `code`, `quote`, `link`, `image`.

`ContentSanitizer` strips HTML/script, keeps plain text, and allows only `http://` / `https://` URLs. The public UI renders blocks as React elements and never injects contributor HTML.

---

## 6. License and sources

License catalog (data): `CC_BY_SA`, `PUBLIC_DOMAIN`, `ORIGINAL_WORK`, `EXTERNAL_ALL_RIGHTS_RESERVED`, `OTHER`.

Selecting a license does not grant rights the contributor does not possess. The editor states this explicitly.

Sources are optional: title, author, publisher, URL, publication info, notes.

---

## 7. Taxonomy administration

`POST /api/v1/admin/subjects` and `POST /api/v1/admin/education-levels` require `TAXONOMY_MANAGE`. Seeded values include Matematika and jenjang SD–Keterampilan. Contributors cannot create public top-level subjects.
