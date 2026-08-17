# Public Content — Belajar Bersama

Anonymous learning reads. No session required.

---

## Visible

- Published, non-archived educational content
- Subject and education-level catalogs
- Contributor **display name**
- Published revision body, summary, license, sources
- Search hits for published material only (title, summary, body text, subject)

---

## Never exposed on public APIs

- Drafts and unpublished revisions
- Workflow status other than published presentation (`status` is always `PUBLISHED` on public DTOs)
- Review comments
- Audit metadata
- Email, auth provider subject, verification evidence

Public DTO uses `publishedRevisionId` as the revision shown even if the maker has a newer draft.

---

## Routes

API: `GET /api/v1/public/subjects`, `/public/content?kind=`, `/public/content/{slug}` (nested children), `/public/courses`, `/public/learning-paths`, `/public/quizzes/{slug}`, `/public/search`.

Web: `/`, `/subjek`, `/subjek/{slug}`, `/kursus/{slug}`, `/jalur/{slug}`, `/materi/{slug}`, `/kuis/{slug}`.
