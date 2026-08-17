# RBAC — Belajar Bersama

Role-based access control as implemented in Phase 3. The permission contract is [PERMISSION_MATRIX.md](PERMISSION_MATRIX.md).

---

## 1. Rules

1. Authorize by **permission + domain policy**, not scattered `if (isAdmin)` checks.
2. A user may hold multiple stored roles. Effective permissions are the union.
3. Domain invariants can still deny an action when a permission is present (maker ≠ checker, competency scope, inactive user).
4. Frontend may hide actions; the API is authoritative.

Runtime source of truth for Role → Permission is `RolePermissionCatalog` in the API. Table `role_permission` is seeded to match and is not queried at request time.

---

## 2. Roles

| Role | How obtained | Stored in `user_role`? |
|---|---|---|
| `LEARNER` | First login | Yes |
| `VERIFIED_CONTRIBUTOR` | Derived from ≥1 `APPROVED` verification | **No** |
| `CHECKER` | Administrator assignment | Yes |
| `MODERATOR` | Administrator assignment | Yes |
| `ADMINISTRATOR` | Administrator assignment, or bootstrap subject list | Yes |

Users cannot assign `CHECKER`, `MODERATOR`, `ADMINISTRATOR`, or `VERIFIED_CONTRIBUTOR` to themselves. `VERIFIED_CONTRIBUTOR` cannot be assigned at all; approve a verification instead. `LEARNER` cannot be assigned or revoked through the admin API.

---

## 3. Effective roles

`GET /api/v1/me` returns:

- `storedRoles` — rows in `user_role`
- `roles` — stored roles plus `VERIFIED_CONTRIBUTOR` when approved verifications exist
- `permissions` — union from the catalog for effective roles

---

## 4. Admin APIs

Privileged user administration (all require authentication + the matching permission):

| Method | Path | Permission |
|---|---|---|
| POST | `/api/v1/admin/users/{id}/roles` | `ROLE_MANAGE` |
| DELETE | `/api/v1/admin/users/{id}/roles/{role}` | `ROLE_MANAGE` |
| POST | `/api/v1/admin/users/{id}/suspend` | `USER_MANAGE` |
| POST | `/api/v1/admin/users/{id}/reactivate` | `USER_MANAGE` |
| POST | `/api/v1/admin/users/{id}/deactivate` | `USER_MANAGE` |

Every assignment, revocation, suspend, reactivate, and deactivate writes an audit event.
