# Course Model — Belajar Bersama

Courses are `EducationalContent` with `kind = COURSE`. They are not a parallel table.

---

## Structure

A course may contain modules, lessons, materials, and quizzes via `parent_id` and `sort_order`.

Example:

```text
Course
  ├── Module 1 (sort_order 1)
  │     ├── Lesson 1
  │     ├── Lesson 2
  │     └── Quiz
  ├── Module 2
  └── Final Quiz
```

Public course payloads include a nested `children` outline of **published** descendants only (title, slug, kind, sortOrder, required).

---

## Authoring

Makers set `kind`, optional `parentId`, `sortOrder`, and `required` on create/update. The same maker–checker publication pipeline as other content applies.

---

## Learning paths

`LEARNING_PATH` is the same tree model one level above courses. Phase 5 does not introduce a many-to-many graph (open decision #18).
