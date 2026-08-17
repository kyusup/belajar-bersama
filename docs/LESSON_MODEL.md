# Lesson Model — Belajar Bersama

Lessons are `EducationalContent` with `kind = LESSON`. Practice items use `kind = MATERIAL`.

---

## Completion

Opening a lesson does **not** complete it.

Authenticated learners call **Tandai sudah selesai** (`POST /api/v1/me/lessons/{contentId}/complete`).

Recorded:

| Field | Meaning |
|---|---|
| user | Authenticated principal |
| content | Lesson or material id |
| revision | Published revision at completion time |
| completedAt | Server timestamp |

Completion is idempotent (`ON CONFLICT DO NOTHING`). Only `LESSON` and `MATERIAL` can be completed. Unpublished content returns `LESSON_NOT_PUBLISHED`.

`POST /api/v1/me/opened/{contentId}` updates resume position without completing the lesson.
