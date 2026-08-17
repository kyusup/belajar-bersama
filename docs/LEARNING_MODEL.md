# Learning Model — Belajar Bersama

Phase 5 learner experience. Educational structure reuses Phase 4 `EducationalContent`; quizzes, attempts, progress, and bookmarks are new.

Related: `COURSE_MODEL.md`, `LESSON_MODEL.md`, `QUIZ_MODEL.md`, `PROGRESS_MODEL.md`, `LEARNER_EXPERIENCE.md`.

---

## Hierarchy

```text
Subject
 └── Learning Path      (kind = LEARNING_PATH, optional)
      └── Course        (kind = COURSE, optional)
           └── Module   (kind = MODULE, optional)
                ├── Lesson / Material
                ├── Practice (kind = MATERIAL)
                └── Quiz (kind = QUIZ)
```

Every node is `EducationalContent`. Parenting is optional `parent_id`. Standalone lessons and quizzes are valid.

A quiz may hang off a lesson, module, or course. Practice is published `MATERIAL`, not a separate CMS.

---

## Published only

Learners see the currently published revision. Public APIs never return drafts, unpublished revisions, review comments, checker notes, or verification evidence.

---

## Ordering

`educational_content.sort_order` is explicit. Children are listed `ORDER BY sort_order, slug`. Insertion order is not used.

---

## Required items

`educational_content.required` (default true) marks whether the node counts toward progress. Quiz specs have a second `required` flag (default true). Optional items are excluded from progress unless both flags are true for quizzes.

---

## Personal data

Progress, attempts, scores, bookmarks, and resume state belong to the authenticated user. Other users receive 404 for attempts and never see another user's bookmarks or progress. Public GETs are cacheable; `/api/v1/me/*` is `private, no-store`.
