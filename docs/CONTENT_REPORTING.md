# Content Reporting — Belajar Bersama

Create path for published educational content. Moderator queue is implemented; see `MODERATION.md`.

---

## Who may report

Authenticated users with `CONTENT_REPORT` (learners). Anonymous reports are not accepted (open decision #11 resolved for MVP: no).

Reports are accepted only for **publicly visible** content. Unpublished ids return not found.

---

## Record

`reporter`, `content`, `reason` (`INCORRECT | COPYRIGHT | INAPPROPRIATE | SPAM | OTHER`), `description`, `status` (`OPEN | UNDER_REVIEW | RESOLVED | DISMISSED`), timestamps.

One open/`UNDER_REVIEW` report per (reporter, content). Duplicates return conflict.

---

## Resolution

Moderators with `CONTENT_REPORT_REVIEW` may list, resolve, or dismiss via `/api/v1/moderation/content-reports`. Q&A reports use `/api/v1/moderation/reports`. UI: `/moderasi`. See `MODERATION.md`.

`POST /api/v1/content/{id}/reports` is the public create path. Web: report form on `/materi/{slug}` for signed-in users.
