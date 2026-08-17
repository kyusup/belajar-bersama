# Quiz Security — Belajar Bersama

---

## Delivery

`GET /api/v1/public/quizzes/{slug}` returns questions and options **without**:

- `correct` flags
- explanations
- scoring internals

Correct answers appear only on a **submitted** attempt owned by the caller (`review` on `GET /api/v1/me/attempts/{id}` and submit response).

---

## Authority

Scoring uses the quiz spec stored for `attempt.quizRevisionId`. Hidden frontend fields and extra JSON (`score: 100`) are ignored.

Invalid option ids or unknown question ids → `INVALID_QUESTION_ANSWER`.

---

## Isolation

| Case | Result |
|---|---|
| Another user's attempt | `ATTEMPT_NOT_FOUND` (404) |
| Submit after submit | `ATTEMPT_ALREADY_SUBMITTED` |
| Exceed `maxAttempts` | `MAX_ATTEMPTS_REACHED` (server count) |
| Concurrent start | Unique open-attempt index; existing open attempt returned |
| Concurrent submit | Optimistic `version` on `quiz_attempt` |

Historical attempts stay pinned to their revision after a later publish.
