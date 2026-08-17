# Authentication — Belajar Bersama

Implemented authentication for Phase 3. Aligns with [ADR-003](adr/ADR-003-authentication-architecture.md).

---

## 1. Architecture

The API is the backend-for-frontend (BFF). It performs OIDC authorization-code + PKCE against Google and Apple, validates ID tokens, maps identity to an application user, and issues a server-side session.

```text
Browser
  → GET /api/v1/auth/{google|apple}/start
  → Identity provider
  → GET /api/v1/auth/{google|apple}/callback
  → Set-Cookie: bb_session (HttpOnly, SameSite=Lax)
  → Redirect to /akun
```

Browser JavaScript never receives OAuth access tokens, refresh tokens, or ID tokens. Those values are not persisted.

Session tokens are stored only as SHA-256 hashes in `auth_session`. Logout and suspend/deactivate revoke sessions.

---

## 2. Providers

| Provider | Production path | Local without credentials |
|---|---|---|
| Google | OIDC when `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are set | Hidden on `/masuk` until configured |
| Apple | OIDC when Apple client/team/key settings are set | Hidden on `/masuk` until configured |
| Username/password | Not implemented; constitution forbids it for this version | — |
| Dev stub | `POST /api/v1/auth/dev/login` | Enabled only when `AUTH_DEV_LOGIN=true` or the `%test` profile |

`GET /api/v1/auth/config` tells the UI which buttons to show: `{ google, apple, devLogin }`.

Redirect URIs registered at the provider must match:

```text
{API_URL}/api/v1/auth/google/callback
{API_URL}/api/v1/auth/apple/callback
```

Local defaults use `http://localhost:8080`.

---

## 3. Session cookie

| Attribute | Value |
|---|---|
| Name | `bb_session` |
| HttpOnly | yes |
| SameSite | Lax |
| Secure | `AUTH_COOKIE_SECURE` (false on local HTTP) |
| Path | `/` |
| TTL | 168 hours (`bb.auth.session-ttl-hours`) |

CORS allows credentials from `CORS_ORIGINS` (default `http://localhost:3000`). Public learning routes do **not** require this cookie.

There is no global “must be authenticated” middleware. Resources call `RequestAuthContext.requireUserId()` when they need a session.

---

## 4. Claim validation

For Google and Apple callbacks the API:

- exchanges `code` with PKCE `code_verifier`
- verifies ID token signature against the provider JWKS (RS256 or ES256)
- checks `iss`, `aud`, and `nonce`
- maps `sub` to `identity_link.subject`, never to `app_user.id`

Invalid or unknown providers return a domain error. Facebook and other social providers are rejected.

---

## 5. Local development stub

When `AUTH_DEV_LOGIN=true`:

```http
POST /api/v1/auth/dev/login
{ "provider": "GOOGLE" | "APPLE", "subject": "…", "displayName": "…", "avatarUrl": null }
```

This still goes through `AuthenticateExternalIdentityService`. It does not skip user/identity creation, default `LEARNER`, or audit events. It **does** skip provider JWT validation. Never enable it in production.

Tests use this stub. Bootstrap administrator: `AUTH_BOOTSTRAP_ADMINS=GOOGLE:admin-1` (already set in the `%test` profile).

---

## 6. Apple limitation

Real Apple Sign-In is implemented (client-secret JWT ES256 + token endpoint + ID-token JWKS). It has not been exercised against Apple’s production/sandbox in this environment. Until `APPLE_*` secrets are present, use the development stub. Apple often omits `name` after the first consent; the user is then stored as `Pengguna` unless a name claim is present.

---

## 7. What is not stored

- OAuth access tokens
- Refresh tokens
- ID tokens
- Client secrets in audit metadata
- Email addresses on `app_user` or identity API responses
