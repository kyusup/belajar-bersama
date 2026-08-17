# API Architecture — Belajar Bersama

REST API conventions for `apps/api`. The MVP implements identity, content workflow, learning, Q&A, moderation, admin, and platform health under `/api/v1`. New resources must follow this document.

---

## 1. Base URL and versioning

- Public HTTP API prefix: `/api/v1`
- Version is in the URL path (`v1`).
- Breaking changes require a new path version (`/api/v2`) or a documented compatibility window.
- Additive, backward-compatible fields may appear in `v1`.
- Public catalog GETs may send `Cache-Control: public, max-age=30`. Personalized `/api/v1/me/*` learning data must send `private, no-store` and must not be mixed into public DTOs.

Ops endpoints (not part of the public product API):

- `/q/health`, `/q/health/live`, `/q/health/ready`
- `/q/openapi`, `/q/swagger-ui` (local/dev)

---

## 2. URL conventions

| Pattern | Meaning |
|---|---|
| `/api/v1/{collection}` | Collection of resources |
| `/api/v1/{collection}/{id}` | Resource by UUID |
| `/api/v1/{collection}/{id}/{sub}` | Subresource |

Use kebab-case collections in English for stability (`/api/v1/learning-paths`), not Indonesian path segments. UI language is Indonesian; API identifiers stay language-stable.

IDs are UUIDs.

---

## 3. HTTP methods

| Method | Use |
|---|---|
| GET | Read; no side effects |
| POST | Create, or non-idempotent action (`/submit`, `/approve`) |
| PUT | Full replace of a resource the client owns |
| PATCH | Partial update |
| DELETE | Remove or request removal where allowed |

Do not use GET for state changes (submit, approve, publish).

---

## 4. Authentication

- Anonymous access is valid for public published learning, health/status, auth config, and the competency catalog.
- Authenticated requests use the HttpOnly session cookie `bb_session` issued by the API after Google/Apple OIDC (or the local dev stub). See `AUTHENTICATION.md`.
- Do **not** send `Authorization: Bearer` for product APIs in this phase. Phase 2 text mentioned Bearer; ADR-003 selected BFF cookies and Phase 3 implemented that. The Bearer sentence was the documentation error.
- Never send provider refresh tokens to the browser.

---

## 5. Authorization

- Enforced only on the backend.
- Use explicit permissions from `PERMISSION_MATRIX.md`, plus domain invariants (competency scope, maker ≠ checker).
- Missing permission → `403` with a stable `code`.
- Unauthenticated where auth is required → `401`.

---

## 6. Error format

All error responses use:

```json
{
  "code": "CONTENT_NOT_REVIEWABLE",
  "message": "Content cannot be reviewed in its current state.",
  "details": {},
  "correlationId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
}
```

| Field | Rule |
|---|---|
| `code` | Stable, machine-readable, `SCREAMING_SNAKE` |
| `message` | Safe for users/clients; no stack traces, SQL, or internal class names |
| `details` | Optional structured field errors or context (no PII dumps) |
| `correlationId` | Mirrors `X-Correlation-Id` |

HTTP status mapping:

| Category | Status | Example codes |
|---|---|---|
| Validation error | 400 | `VALIDATION_FAILED` |
| Authentication required | 401 | `UNAUTHENTICATED` |
| Authorization error | 403 | `FORBIDDEN`, `USER_NOT_VERIFIED_FOR_COMPETENCY` |
| Not found | 404 | `RESOURCE_NOT_FOUND` |
| Conflict | 409 | `CONTENT_ALREADY_PUBLISHED`, `CONFLICT` |
| Business rule violation | 422 | `MAKER_CANNOT_REVIEW_OWN_CONTENT`, `INVALID_CONTENT_TRANSITION` |
| Rate limited | 429 | `RATE_LIMITED` |
| Infrastructure failure | 503 | `INFRASTRUCTURE_FAILURE` |
| Unexpected failure | 500 | `UNEXPECTED_FAILURE` |

`422` is used for domain rule failures that are syntactically valid but not allowed. Do not collapse these into generic `500`.

---

## 7. Validation errors

Field-level validation:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "details": {
    "fields": [
      { "field": "title", "code": "REQUIRED", "message": "Title is required." }
    ]
  },
  "correlationId": "..."
}
```

---

## 8. Pagination, filtering, sorting

When list endpoints exist:

**Pagination (offset):**

```text
GET /api/v1/resources?page=0&size=20
```

Response envelope:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0
}
```

Defaults: `page=0`, `size=20`, max `size=100`.

**Filtering:** explicit query parameters (`status=PUBLISHED`, `subjectId=`). Do not accept raw SQL/JSON query languages from clients.

**Sorting:** `sort=updatedAt,desc`. Allow-list sortable fields per resource. Unknown sort fields → `VALIDATION_FAILED`.

---

## 9. Correlation / request IDs

- Client may send `X-Correlation-Id` or `X-Request-Id`.
- If absent, the API generates a UUID.
- The value is returned as `X-Correlation-Id` and included in logs (MDC) and error bodies.
- Prefer one ID per user-visible request; the web client should generate and send it.

---

## 10. OpenAPI

Quarkus generates OpenAPI from JAX-RS annotations (`/q/openapi`).

TypeScript types in `packages/shared` are hand-aligned for this skeleton. Automated client generation may be added later; it is not required for Phase 2.

---

## 11. Learning APIs (Phase 5)

Public (cacheable, published only):

- `GET /api/v1/public/content?subject=&kind=`
- `GET /api/v1/public/learning-paths`, `/public/courses`, `/public/content/{slug}` (includes nested children)
- `GET /api/v1/public/quizzes/{slug}` (no answer keys)
- `GET /api/v1/public/search?q=` (published content and non-hidden Q&A)

Authenticated (`bb_session`, `Cache-Control: private, no-store`):

- `GET /api/v1/me/progress/{contentId}`
- `POST /api/v1/me/lessons/{contentId}/complete`
- `POST /api/v1/me/opened/{contentId}`
- `GET /api/v1/me/continue`
- `GET|POST /api/v1/me/bookmarks`, `DELETE /api/v1/me/bookmarks/{contentId}`
- `POST /api/v1/me/quizzes/{quizId}/attempts`
- `GET /api/v1/me/quizzes/{quizId}/attempts`
- `GET /api/v1/me/attempts/{id}`
- `POST /api/v1/me/attempts/{id}/answers`
- `POST /api/v1/me/attempts/{id}/submit`
- `GET /api/v1/me/quiz-history`

Learning error codes include `QUIZ_NOT_PUBLISHED`, `ATTEMPT_NOT_FOUND`, `ATTEMPT_ALREADY_SUBMITTED`, `MAX_ATTEMPTS_REACHED`, `INVALID_QUESTION_ANSWER`, `LESSON_NOT_PUBLISHED`, `CONTENT_NOT_AVAILABLE`.

Makers create quizzes with `POST /api/v1/content` `kind=QUIZ` and a `quiz` object. Same submit/review/publish pipeline as other content.

---

## 12. Q&A and moderation APIs (Phase 6)

Public (cacheable, non-hidden only):

- `GET /api/v1/public/qa?contentId=&subjectId=&page=`
- `GET /api/v1/public/qa/{id}`
- `GET /api/v1/public/search?q=` also returns `QA_QUESTION` hits (slug = question id)

Authenticated (`bb_session`, `private, no-store`):

- `POST /api/v1/qa` `{ title, body, subjectId?, contentId? }` (`contentId` must be published)
- `PATCH /api/v1/qa/{id}` author only (foreign ids → 404)
- `POST /api/v1/qa/{id}/close`
- `POST /api/v1/qa/{id}/answers`
- `PATCH /api/v1/qa/answers/{id}`
- `POST /api/v1/qa/{id}/accept/{answerId}`, `DELETE /api/v1/qa/{id}/accept`
- `POST|DELETE /api/v1/qa/answers/{id}/useful`
- `POST /api/v1/qa/{id}/reports`, `POST /api/v1/qa/answers/{id}/reports`

Moderation (`CONTENT_MODERATE` / `CONTENT_REPORT_REVIEW`, not under `/admin`):

- `GET /api/v1/moderation/reports`
- `GET /api/v1/moderation/qa/{id}`
- `POST /api/v1/moderation/qa/{id}/hide`
- `POST /api/v1/moderation/qa/answers/{id}/hide`
- `POST /api/v1/moderation/reports/{id}/resolve|dismiss`
- `GET /api/v1/moderation/content-reports`
- `POST /api/v1/moderation/content-reports/{id}/resolve|dismiss`

Q&A error codes include `QA_NOT_FOUND`, `QA_CLOSED`, `QA_NOT_AUTHOR`, `ANSWER_NOT_FOUND`, `CANNOT_MARK_OWN_ANSWER`.

---

## 13. Abuse control (Phase 7)

- Mutating requests with a disallowed `Origin`/`Referer` → `403 CSRF_ORIGIN_DENIED`
- Rate limits → `429 RATE_LIMITED` (see `SECURITY_ARCHITECTURE.md`)
- `GET /api/v1/admin/users?q=` lists users for `USER_MANAGE` (display name, status, stored roles; no email or identity ids)
