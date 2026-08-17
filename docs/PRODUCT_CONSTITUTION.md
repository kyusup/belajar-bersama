# Product Constitution — Belajar Bersama

This document is the highest-level product authority for Belajar Bersama.  
All product, governance, domain, and architecture decisions must remain compatible with it.

If a lower-level document conflicts with this constitution, this constitution prevails until an explicit amendment is approved.

---

## 1. Mission

Belajar Bersama is an open learning platform and knowledge community for Indonesian society.

Its purpose is to make learning, knowledge, and quality educational resources more accessible, supporting both formal and informal education, and helping people become independent learners.

---

## 2. Product Identity

| Decision | Value |
|---|---|
| Product name | Belajar Bersama |
| Project nature | Open-source |
| Primary public repository | GitHub |
| Primary audience | Indonesian society |
| Education scope | Formal and informal |
| UI language (initial) | Indonesian |
| Primary content language (initial) | Indonesian |

Foreign-language lessons may use the language being taught. Architecture must not unnecessarily block future localization.

---

## 3. Foundational Product Rules

1. **Accessibility first** — Public learning content must be accessible without authentication.
2. **Quality over quantity** — Educational quality takes precedence over content volume.
3. **Independent learning** — The platform should help users learn independently, not create dependency on the product or on AI.
4. **Verified contribution** — Only verified contributors may create educational content within their competency scope.
5. **Reviewed publication** — Educational content is published only through maker–checker review, never by unchecked self-publish.
6. **No self-approval** — A maker must never approve their own submission.
7. **Privacy by default** — Collect and expose only necessary personal data, especially considering use by minors.
8. **Knowledge before AI** — Curated, reviewed educational knowledge is foundational. AI is not an MVP requirement and must not become the source of educational truth.
9. **Public learning remains free** — MVP must not introduce paywalls, subscriptions, advertising, or monetized access to learning content.
10. **Not a school LMS only** — The platform must support school-aligned and non-school learning without being hardcoded as a traditional LMS.

---

## 4. Access Model

| Capability | Anonymous | Authenticated learner | Verified contributor | Checker / Moderator / Admin |
|---|---|---|---|---|
| Browse/consume public learning | Yes | Yes | Yes | Yes |
| Progress, bookmarks, quiz history | No | Yes | Yes | Yes |
| Ask/answer Q&A (permitted activities) | No | Yes | Yes | Yes |
| Create educational content | No | No | Yes (scoped) | Per role |
| Review educational content | No | No | No | Checker (eligible) |
| Platform governance | No | No | No | Administrator |

Exact permission boundaries are defined in `ROLE_PERMISSION_CONCEPT.md` and must remain consistent with this table.

---

## 5. Authentication (Initial Version)

- Supported: Google Sign-In, Apple Sign-In
- Not supported in initial version: username/password authentication

Authentication is required only for personalized or contribution features.

---

## 6. Content and Knowledge Hierarchy (Long-term)

```text
Subject
  → Learning Path
    → Course
      → Module
        → Lesson
          → Practice / Quiz
            → Progress
```

Mastery by concept is a long-term capability. Adaptive learning and AI tutoring are out of scope for MVP.

Intended future AI direction:

```text
Verified Content → Reviewed Knowledge → Knowledge Base → AI Learning Assistant
```

---

## 7. Governance Hierarchy

Conceptual roles (a user may hold multiple roles):

- Learner
- Verified Contributor / Maker
- Checker
- Moderator
- Administrator

Logical separation: Maker and Checker must remain separated for the same content submission.

Details: `GOVERNANCE.md`, `CONTENT_GOVERNANCE.md`, `VERIFICATION.md`, `MAKER_CHECKER.md`.

---

## 8. Licensing Separation

Software licensing and educational-content licensing are separate concepts.

- Recommended source-code license: Apache License 2.0
- Recommended educational-content license (where applicable and lawful): CC BY-SA

Third-party materials must not be assumed relicensable. Details: `COPYRIGHT_AND_CONTENT_LICENSE.md`.

---

## 9. Funding Stance (MVP)

Initial development and infrastructure are funded personally by the project founder.  
Future sustainability may include donations/community support.

MVP must not implement monetization, advertising, subscriptions, or paywalls.

---

## 10. Amendment Rule

Changes to this constitution require explicit product decision and documentation update.  
Do not silently reinterpret foundational rules during implementation.

Unresolved product choices belong in `OPEN_DECISIONS.md`.
