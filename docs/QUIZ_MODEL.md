# Quiz Model — Belajar Bersama

Quizzes are `EducationalContent` with `kind = QUIZ`. Question definitions live on **the content revision**, not the content identity.

---

## Tables

| Table | Role |
|---|---|
| `quiz_spec` | Per-revision passing score, max attempts, required |
| `quiz_question` | Prompt, type, explanation, difficulty, order |
| `quiz_option` | Label, text, correct flag |
| `quiz_attempt` | Per-user attempt pinned to `quiz_revision_id` |
| `quiz_answer_option` | Selected options for an attempt |

---

## Question types (MVP)

| Type | Rule |
|---|---|
| `SINGLE_CHOICE` | Exactly one correct option |
| `MULTIPLE_CHOICE` | One or more correct options |
| `TRUE_FALSE` | Exactly two options, exactly one correct |

Difficulty (`EASY | MEDIUM | HARD`) is metadata only. No adaptive selection.

Ordering is `sort_order` on questions and options. MVP delivery uses configured order, not randomization.

---

## Attempts

Status: `IN_PROGRESS | SUBMITTED | ABANDONED` (abandoned unused in MVP).

- Starting a quiz reuses an existing `IN_PROGRESS` attempt (idempotent). Unique index: one open attempt per `(user, quiz)`.
- `maxAttempts` null = unlimited. Count includes `IN_PROGRESS` and `SUBMITTED`.
- After submit, answers and score are immutable. Retry creates a new attempt.
- The client cannot set the score. The server scores against the **attempt's quiz revision**.

See `ASSESSMENT_RULES.md` and `QUIZ_SECURITY.md`.
