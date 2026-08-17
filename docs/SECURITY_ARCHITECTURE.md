# Security Architecture — Belajar Bersama

Security baseline as implemented from Phase 3. Aligns with `PRIVACY_PRINCIPLES.md` and `PRODUCT_CONSTITUTION.md`.

---

## 1. Threat posture (MVP-relevant)

- Public read of published learning is intentional.
- Write/contribute/review paths must be authenticated, authorized, competency-scoped, and audited.
- Minors may use the platform: minimize PII, no private DMs, no public emails.
- The browser is untrusted. Frontend checks are UX only.

---

## 2. OAuth / OIDC

- Providers: Google Sign-In, Apple Sign-In only for the initial version.
- No username/password credential store.
- Flow: OIDC authorization code with PKCE **inside the API** (BFF). Session cookie afterwards (ADR-003).
- Map `iss` + `sub` → `identity_link` → `app_user`. Never use `sub` as `User.id`.
- Details: `AUTHENTICATION.md`.

---

## 3. Session / token handling

- HTTP-only, SameSite=Lax cookie `bb_session`. `Secure` when `AUTH_COOKIE_SECURE=true`.
- Session token hashed with SHA-256 at rest; raw token only in the cookie.
- Logout, suspend, and deactivate revoke sessions.
- Inactive users cannot resolve a session even if a cookie remains.
- Do not store refresh tokens in `localStorage`.
- Do not log tokens, authorization headers, ID tokens, or secrets.
- OAuth tokens are not persisted.

---

## 4. CSRF

- SameSite=`Lax` on the session cookie.
- CORS allow-list (`CORS_ORIGINS`); credentials allowed; no wildcard origin.
- Frontend mutating calls use `fetch` with `credentials: 'include'` from the allowed origin.
- Mutating requests (`POST`/`PUT`/`PATCH`/`DELETE`) that send `Origin` or `Referer` are rejected unless the origin is in `CORS_ORIGINS` or `APPLICATION_URL` (`CSRF_ORIGIN_DENIED`). Requests with neither header (non-browser clients, tests) are allowed.
- A dedicated anti-CSRF synchronizer token is still not implemented; Origin/Referer plus SameSite=Lax is the current browser CSRF control.

---

## 5. CORS

CORS is **explicit** and configuration-driven:

- Allowed origins from `CORS_ORIGINS` (comma-separated)
- Local default: `http://localhost:3000`
- Methods and headers allow-listed
- No wildcard origin in the committed defaults

---

## 6. Input validation

- Validate at the API boundary (Bean Validation) and re-assert invariants in the domain.
- Reject unknown fields where practical.
- Bound string lengths and collection sizes.
- UUID path params must be typed UUIDs.

---

## 7. Authorization

- Backend-only enforcement using permissions (`PERMISSION_MATRIX.md`) plus domain policies (`AUTHORIZATION_POLICIES.md`).
- Do not scatter `if (user.isAdmin)` as the primary model.
- Anonymous is a first-class principal for public reads.
- Suspended/deactivated users cannot perform protected actions.

---

## 8. Rate limiting

Implemented in the API as a Quarkus JAX-RS filter (single-process in-memory fixed window). Not a gateway product.

| Bucket | Default | Paths |
|---|---|---|
| AUTH | 20/min | Mutating `/api/v1/auth/*` |
| WRITE | 60/min | Other POST/PUT/PATCH/DELETE |
| REPORT | 10/min | `POST .../reports` |
| SEARCH | 40/min | `GET /api/v1/public/search` |
| PUBLIC | 120/min | Other `/api/v1/public/*` GETs |

Health, status, auth config, and `/q/*` are excluded. Identity is `user:{id}` when a session resolves, otherwise client IP. `X-Forwarded-For` is **not** trusted unless `bb.rate-limit.trust-forwarded-for=true` behind a known proxy.

Exceeding a bucket returns `429` `RATE_LIMITED` with `Retry-After`. Multi-instance deployments need a shared store later; fail-closed if a limit is configured as `0`.

---

## 9. File upload security

See also ADR-005. Rules even before product upload UI exists:

- Allow-list MIME types (e.g. image/jpeg, image/png, application/pdf) — never `*/*`
- Enforce size limits
- Store using generated object keys, not user filenames
- Serve downloads as attachments / non-executable content types; never execute uploads
- Isolate objects by prefix (`content/{contentId}/...`)
- Malware scanning is a **future** capability (document hook only)

---

## 10. Content sanitization

- Treat contributor and Q&A text as untrusted input.
- `ContentSanitizer` strips HTML/scripts on create and review (paragraph blocks, plain text fields, Q&A bodies).
- Rich-text allow-lists may expand later; default is strict plain-text normalization for MVP.

Implemented in `domain/content/ContentSanitizer.java` and applied in content and Q&A services.

---

## 11. Secret management

- All secrets via environment / secret store, never committed.
- `.env` is gitignored; `.env.example` has placeholders only.
- Production should use platform secrets (not `.env` files in images).

---

## 12. Security headers (web)

Next.js should send, at least when pages exist:

- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-Frame-Options: DENY`
- `Content-Security-Policy` (`default-src 'self'`, `frame-ancestors 'none'`, `connect-src` includes the API origin)
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`

---

## 13. Audit logging

- Governance and security-relevant actions → `AuditRecorder` / `audit_event`
- Application logs are structured JSON in Quarkus; include `correlationId`
- Do not write emails, tokens, or raw ID tokens to audit metadata

---

## 14. PII minimization

- Identity provider emails stay on `Identity` (private), not on public User projections
- Public APIs must not return email or auth subject identifiers
- Collect only what constitution/privacy docs allow

---

## 15. Error disclosure

- Exception mappers convert failures to the public error format
- Stack traces only in server logs
- Generic `UNEXPECTED_FAILURE` for unknown errors
