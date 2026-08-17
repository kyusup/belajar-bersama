# Identity Architecture — Belajar Bersama

Implemented identity model for Phase 3. Product authority remains `PRODUCT_CONSTITUTION.md`.

This document describes **actual** runtime behavior, not a future design.

---

## 1. Distinctions that must be preserved

```text
Identity ≠ User
User ≠ Role
Role ≠ Permission
Verification ≠ Role
Competency ≠ Verification
```

| Concept | What it is | What it is not |
|---|---|---|
| External identity | Google or Apple `iss` + `sub`, stored on `identity_link` | The application's user id |
| Application user | Stable UUID owned by Belajar Bersama (`app_user.id`) | A provider subject |
| Role | Named authority (`LEARNER`, `CHECKER`, …) | A permission or a verification |
| Permission | Explicit capability checked by the API | A UI flag |
| Competency | Managed taxonomy row (Mathematics, Java, …) | An enum hardcoded in Java |
| Verification | Competency-scoped application/decision | `user.isVerified` |

---

## 2. Mapping flow

```text
Google / Apple (OIDC)
        ↓
Validated ID token claims (iss, sub, name, picture)
        ↓
Identity mapping (provider + issuer + subject)
        ↓
Application User
        ↓
Stored roles + derived VERIFIED_CONTRIBUTOR
        ↓
Permissions + competency-scoped policies
```

Provider-specific OAuth/OIDC code lives in `infrastructure.auth`. Domain services receive `ExternalIdentityClaims` only.

---

## 3. First login

1. Look up `identity_link` by `(provider, issuer, subject)`.
2. If found, load that `app_user` (no second user is created).
3. If not found:
   - create `app_user` with a new UUID and status `ACTIVE`
   - create `identity_link`
   - assign stored role `LEARNER`
   - write `USER_CREATED` and `IDENTITY_LINKED` audit events
4. If the identity matches `bb.auth.bootstrap-admin-subjects` (form `GOOGLE:subject`), assign `ADMINISTRATOR` and audit `ROLE_ASSIGNED`.

Default display name is `Pengguna` when the provider does not supply a name.

---

## 4. Identity linking

The schema allows one user to have both a Google and an Apple row:

```text
User
 ├── Google identity_link
 └── Apple identity_link
```

Automatic merge by matching email is **not** implemented. Linking must be a future deliberate, authenticated action. Two logins with the same email but different providers currently create two users (open decision #16).

The identity API DTO exposes `id`, `provider`, and `issuer` only. Provider `sub` is not returned to the browser.

---

## 5. User lifecycle

| Status | Meaning | Session |
|---|---|---|
| `ACTIVE` | May perform authorized actions | Valid until expiry/logout |
| `SUSPENDED` | Temporarily blocked | All sessions revoked; resolve rejects the user |
| `DEACTIVATED` | Permanently blocked | Same as suspended |

A valid Google/Apple account does not override these statuses. Protected APIs require an active application user.

---

## 6. Derived contributor role

`VERIFIED_CONTRIBUTOR` is **not** stored in `user_role`.

It is added to the effective role set when the user has at least one `APPROVED` verification. Contributor eligibility is still competency-scoped; the derived role only grants the permission names (`CONTENT_CREATE`, …). Creating content for a competency still requires an approved verification for **that** competency.

Privileged stored roles (`CHECKER`, `MODERATOR`, `ADMINISTRATOR`) are assigned only by an administrator with `ROLE_MANAGE`. Users cannot assign those roles to themselves. `LEARNER` is assigned on first login only.
