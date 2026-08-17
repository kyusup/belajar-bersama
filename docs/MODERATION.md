# Moderation — Belajar Bersama

Report → review → resolve. Hide, do not silently delete educational or Q&A history.

---

## Scope (this phase)

- Q&A questions and answers
- Published educational content reports (queue + resolve/dismiss)

Not implemented: private messaging, public user profiles, reputation, automated takedown of published curriculum revisions (archive remains a maker/admin content operation).

---

## Permissions

| Permission | Role | Capability |
|---|---|---|
| `CONTENT_REPORT` | LEARNER | Create a report |
| `CONTENT_MODERATE` | MODERATOR | Hide Q&A; accept/close Q&A; Q&A report queue |
| `CONTENT_REPORT_REVIEW` | MODERATOR | List/resolve/dismiss educational content reports |

Administrators do not get these by default. Assign `MODERATOR` separately.

Anonymous reports are not accepted (open decision #11).

---

## Q&A reports

`POST /api/v1/qa/{id}/reports` and `POST /api/v1/qa/answers/{id}/reports`

Reasons: `INCORRECT | COPYRIGHT | INAPPROPRIATE | SPAM | OTHER`

One open/`UNDER_REVIEW` report per (reporter, target). Duplicates → 409.

Moderators:

- `GET /api/v1/moderation/reports`
- `POST /api/v1/moderation/reports/{id}/resolve|dismiss`
- `POST /api/v1/moderation/qa/{id}/hide`
- `POST /api/v1/moderation/qa/answers/{id}/hide`
- `GET /api/v1/moderation/qa/{id}` (includes hidden answers; still no emails)

Hiding a question removes it from public GET and search. Hiding an accepted answer clears `acceptedAnswerId`.

---

## Educational content reports

Create: `POST /api/v1/content/{id}/reports` (published content only).

Queue: `GET /api/v1/moderation/content-reports`

Resolve/dismiss: `POST /api/v1/moderation/content-reports/{id}/resolve|dismiss`

Reporter UUID is visible to reviewers; email and auth identifiers are not.

---

## Audit

`QA_REPORTED`, `QA_HIDDEN`, `CONTENT_REPORTED`, `CONTENT_REPORT_RESOLVED`, `CONTENT_REPORT_DISMISSED`.

Do not log report description beyond what the audit metadata already stores (target ids, reason).
