# Assessment Rules — Belajar Bersama

Deterministic MVP scoring. No partial credit. No AI grading.

---

## Evaluation

The server loads the quiz spec for `attempt.quizRevisionId` and compares submitted option ids to `correct` flags in the database.

A question is correct **iff** the submitted option-id set **exactly equals** the correct option-id set.

Empty submission for a question is incorrect.

---

## Score

```text
percent = floor(correctQuestions * 100 / questionCount)
```

`questionCount` is the number of questions on that revision. Client-supplied `score` fields are ignored.

---

## Pass / fail

If `passingScore` is null, `passed` is null. The product does **not** invent a default passing score.

If `passingScore` is set (0–100), `passed = percent >= passingScore`.

---

## Progress contribution

If the quiz is required:

- with a passing score: a `SUBMITTED` attempt with `passed = true` counts as complete
- without a passing score: any `SUBMITTED` attempt counts as complete

Not every quiz blocks continuation. Authors set `required` on the content node and quiz spec.
