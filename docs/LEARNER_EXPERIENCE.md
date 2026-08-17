# Learner Experience — Belajar Bersama

Indonesian UI. No global auth middleware. Anonymous users can browse and read published content.

---

## Public

| Page | Path |
|---|---|
| Home | `/` subjects, recent courses, search, continue if signed in |
| Subjects | `/subjek`, `/subjek/{slug}` |
| Course / path | `/kursus/{slug}`, `/jalur/{slug}` |
| Lesson / material / module | `/materi/{slug}` |
| Quiz | `/kuis/{slug}` |
| Result | `/kuis/{slug}/hasil/{attemptId}`, `/hasil/{attemptId}` |
| Q&A | `/tanya`, `/tanya/{id}` |

---

## Authenticated

| Page | Path |
|---|---|
| Dashboard | `/belajar` continue, recent quiz results, bookmarks |
| Lesson actions | Tandai sudah selesai, bookmark, tautan tanya jawab |
| Quiz | start, answer, submit, review, try again |
| Moderation | `/moderasi` for `CONTENT_MODERATE` / `CONTENT_REPORT_REVIEW` |
| Account | `/akun` including competency verification apply |
| Admin | `/kelola` verification, users (display name), roles, taxonomy |

---

## Accessibility

Semantic headings, labels, fieldset/legend for quiz options, visible `:focus-visible`, keyboard radios/checkboxes, progress as text plus a progressbar (not color alone).

Primary flow is usable on a narrow viewport (header wraps, stacked search, full-width options).

---

## Caching

Public content/quiz GETs send `Cache-Control: public, max-age=30`. Personalized `/api/v1/me/*` sends `private, no-store`. Progress is never mixed into public DTOs.
