# Content API — Belajar Bersama

REST under `/api/v1`, cookie session `bb_session`, no global auth filter. Public GETs stay anonymous.

Error body: `{ code, message, details, correlationId }`.

---

## Catalog

| Method | Path | Auth |
|---|---|---|
| GET | `/subjects`, `/education-levels`, `/competencies`, `/licenses` | Public |
| GET | `/public/subjects`, `/public/subjects/{slug}` | Public |
| GET | `/public/content`, `/public/content/{slug}`, `/public/courses/{slug}` | Public |
| GET | `/public/learning-paths`, `/public/courses` | Public |
| GET | `/public/quizzes/{slug}` | Public; no answer keys |
| GET | `/public/search?q=` | Public |

---

## Maker

| Method | Path | Notes |
|---|---|---|
| POST | `/content` | Create draft |
| GET | `/content/{id}` | Owner/assigned checker: current revision; others: published only or 404 |
| PATCH | `/content/{id}` | Edit body/metadata only — **not** status |
| POST | `/content/{id}/submit` | |
| POST | `/content/{id}/publish` | `APPROVED` only |
| POST | `/content/{id}/archive` | Soft archive |
| GET | `/my/content` | |
| GET | `/my/content/{id}/revisions` | Owner only |
| POST | `/content/{id}/reports` | Authenticated reporter |

Learner (authenticated, own data only): see `API_ARCHITECTURE.md` §11 and `LEARNER_EXPERIENCE.md`.

---

## Checker

`{id}` on review routes is the **submission** id.

| Method | Path |
|---|---|
| GET | `/reviews/my` |
| GET | `/reviews/{id}` |
| POST | `/reviews/{id}/start` |
| POST | `/reviews/{id}/approve` |
| POST | `/reviews/{id}/request-changes` |

---

## Admin

| Method | Path |
|---|---|
| POST | `/admin/content/{id}/assign-reviewer` |
| POST | `/admin/subjects` |
| POST | `/admin/education-levels` |

---

## Audit actions

`CONTENT_CREATED`, `CONTENT_UPDATED`, `CONTENT_SUBMITTED`, `CONTENT_REVIEW_ASSIGNED`, `CONTENT_REVIEW_STARTED`, `CONTENT_CHANGES_REQUESTED`, `CONTENT_APPROVED`, `CONTENT_PUBLISHED`, `CONTENT_ARCHIVED`, `CONTENT_REPORTED`, `CONTENT_REPORT_RESOLVED`, `CONTENT_REPORT_DISMISSED`.
