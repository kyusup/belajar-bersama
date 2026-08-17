# Q&A Model — Belajar Bersama

Community learning questions and answers. Not a social network. Not maker–checker curriculum.

Implemented in Phase 6.

---

## Purpose

Learners ask and answer questions about published learning material or a subject. Public visitors may **read** non-hidden threads without authentication.

Q&A is **not** educational content publication. It does not require competency verification. It is not reviewed by Checkers.

---

## Entities

```text
QaQuestion
  author, title, body (plain text), optional subject, optional published content
  status: OPEN | CLOSED | HIDDEN
  acceptedAnswerId

QaAnswer
  question, author, body (plain text), hidden

QaAnswerUseful
  one mark per (user, answer)

QaReport
  reporter, QUESTION|ANSWER, reason, description, OPEN|UNDER_REVIEW|RESOLVED|DISMISSED
```

Hide, do not hard-delete. Hidden questions return not found on public APIs and are removed from search.

---

## Who may do what

| Action | Who |
|---|---|
| Read non-hidden Q&A | Anyone |
| Ask | Authenticated `QA_ASK` (learners) |
| Answer | Authenticated `QA_ANSWER` while question is `OPEN` |
| Edit own question/answer | Author (hidden/foreign ids → 404) |
| Close | Asker or `CONTENT_MODERATE` |
| Accept / unaccept | **Asker of that question**, or `CONTENT_MODERATE` |
| Mark useful | Authenticated `QA_MARK_USEFUL`; cannot mark own answer; one mark per user |
| Report | Authenticated `CONTENT_REPORT`; one open report per (user, target) |
| Hide | `CONTENT_MODERATE` |

`QA_ACCEPT_ANSWER` exists in the permission catalog but is **not granted** to any role. Acceptance is ownership + moderator, not a global “accept anyone’s answer” grant.

Administrators do **not** receive `CONTENT_MODERATE` by default.

---

## Accepted answer (open decision #13, MVP)

Hybrid:

- The asker may accept or unaccept an answer on **their own** question.
- A moderator with `CONTENT_MODERATE` may accept or unaccept.
- Other learners receive `QA_NOT_AUTHOR` (422).

Useful marks are not votes, reputation, or a leaderboard.

---

## Text safety

Titles and bodies are plain text via `ContentSanitizer.plainText` (tags stripped). Max title 200, body 8000.

Public payloads include **display name only** — no email, no auth provider ids.

---

## Search

PostgreSQL `qa_search` is unioned into public search. Hits use `type = QA_QUESTION` and `slug = question id`. Hidden questions are not indexed / not returned.

---

## Web

| Page | Path |
|---|---|
| List / ask | `/tanya` (`?content=` optional) |
| Thread | `/tanya/{id}` |
| Moderation | `/moderasi` |

Lesson pages link “Tanya tentang materi ini”.
