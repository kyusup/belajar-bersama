# Progress Model — Belajar Bersama

Progress is **computed**, not a manually editable percentage. Derived values are not stored.

---

## Required items

For a published root (path, course, module, lesson, quiz):

1. Collect published descendants (recursive `parent_id` tree), plus the root when it is itself a completable item.
2. Skip the root when it is `LEARNING_PATH`, `COURSE`, or `MODULE`.
3. Keep items with `required = true` that are:
   - `LESSON` or `MATERIAL`, or
   - `QUIZ` whose spec is missing or `spec.required = true`.

---

## Percentage

```text
percent = floor(completedRequired / totalRequired * 100)
```

If `totalRequired = 0`, percent is 0.

A lesson/material is completed when `lesson_completion` exists for that user and content.

A required quiz is completed per `ASSESSMENT_RULES.md`.

---

## Resume

`learning_resume` stores one row per user: last opened published content and nearest course id. Updated on lesson complete, quiz start, and `POST /api/v1/me/opened/{contentId}`.

Continue learning returns that content if it is still publicly visible. No recommendation engine.
