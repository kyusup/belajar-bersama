# Verification — Belajar Bersama

Verification grants competency-scoped permission to create educational content.  
It is not a single global “trusted user” flag.

---

## 1. Anti-Pattern (Forbidden Model)

Do not model verification as only:

```text
user.isVerified = true
```

A user verified for one competency must not automatically become verified for unrelated competencies.

---

## 2. Conceptual Model

```text
User
 └── Verification(s)
      ├── Competency: Mathematics
      ├── Competency: Statistics
      └── Competency: Education
```

Each verification binds a user to a competency (and related eligibility metadata as needed).

---

## 3. Effects of Verification

A verification may allow a user to act as Maker for educational content in that competency scope.

Verification does not by itself:

- publish content
- grant checker rights for all domains
- grant administrator rights
- waive copyright/attribution requirements

Checker eligibility may be related to competency but is a distinct authorization concern. See `ROLE_PERMISSION_CONCEPT.md`.

---

## 4. Initial Authority

Initial verification is administered by the platform Administrator / governance role.

Self-service “claim verification” without admin/governance approval is not part of the initial model.

---

## 5. Future Evidence (Domain-Ready, UI Later)

The domain should allow future attachment of evidence such as:

- credentials
- education history
- professional experience
- institutional affiliation
- other competency evidence

MVP must not over-engineer verification evidence UI/workflows.

Status of required evidence types for granting verification: `OPEN DECISION`.

---

## 6. Lifecycle Concerns

Verification records should support at least:

- grant
- scope (competency)
- granting authority/actor
- timestamps
- revocation / suspension (needed for integrity)

Exact revocation policy and appeal process: `OPEN DECISION`.

---

## 7. Invariants

1. Verification is always competency-scoped.
2. Content creation authorization must check active verification for the relevant competency.
3. Unrelated competencies are not implied by a single verification.
4. Verification changes should be auditable.
