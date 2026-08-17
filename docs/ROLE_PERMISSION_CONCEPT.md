# Role & Permission Concept — Belajar Bersama

Conceptual authorization model. Not a complete RBAC implementation matrix for every API.

---

## 1. Principles

1. Prefer capability checks tied to domain invariants over scattered UI hiding.
2. Users may hold multiple roles.
3. Role stacking never permits maker self-approval on the same submission.
4. Anonymous access is a first-class mode for public learning reads.
5. Contribution rights come from **Verification (competency-scoped)**, not merely “authenticated”.

---

## 2. Conceptual Roles

| Role | How obtained | Primary powers |
|---|---|---|
| Learner | Default for authenticated users; anonymous has read-only public subset | Consume public content; authenticated personalization/Q&A as allowed |
| Verified Contributor (Maker) | Active Verification for competency | Create/edit drafts, submit/resubmit within scope |
| Checker | RoleAssignment + eligibility for competency/domain | Review eligible submissions; decide approve / changes requested |
| Moderator | RoleAssignment | Handle reports; moderate Q&A/content per policy |
| Administrator | RoleAssignment | Verification grants, taxonomy, roles, platform configuration |

---

## 3. Permission Concept Matrix

Legend: A = Anonymous, L = Authenticated Learner, M = Maker (verified scope), C = Checker (eligible), Mod = Moderator, Adm = Admin

| Capability | A | L | M | C | Mod | Adm |
|---|---|---|---|---|---|---|
| Read published educational content | Yes | Yes | Yes | Yes | Yes | Yes |
| Track progress / bookmarks / quiz history | No | Own | Own | Own | Own | Own / governance as needed |
| Ask / answer Q&A | No | Yes | Yes | Yes | Yes | Yes |
| Create educational drafts | No | No | Yes (scoped) | No* | No* | No* |
| Submit for review | No | No | Yes (own, scoped) | No | No | No* |
| Review submission | No | No | No | Yes (eligible, not maker) | No | `OPEN DECISION` |
| Approve own submission | No | No | **Never** | **Never** | **Never** | **Never** as self-approval bypass disguised as maker |
| Publish via lifecycle | No | No | After `APPROVED` | No | No | Admin may publish an already-approved item; force-publish unapproved is `OPEN DECISION` #1 |
| Moderate reports | No | No | No | Limited if also Mod | Yes | Yes |
| Grant verification | No | No | No | No | No | Yes |
| Maintain taxonomy | No | No | No | No | No | Yes |
| Assign roles | No | No | No | No | No | Yes |

\*Unless the same user also holds Maker/Admin and is acting under those roles with invariants still enforced.

---

## 4. Scope Rules

### Maker scope

Allowed only when:

- user has active Verification for competency C
- content is classified under competency/subject scope that maps to C

### Checker scope

Allowed only when:

- user has Checker role (or equivalent assignment)
- user is eligible for competency/domain D of the submission (Phase 3: an `APPROVED` verification for that competency)
- user is not the maker of that submission

Exact data model for “checker eligibility”: **resolved in Phase 3** as Checker role + approved verification + not maker. See `AUTHORIZATION_POLICIES.md`.

---

## 5. Authorization Boundaries (implementation intent)

- **Public API boundary:** published content reads
- **User boundary:** progress/bookmarks/history owned by principal
- **Contribution boundary:** verification + lifecycle state machine
- **Review boundary:** eligibility + anti-self-approval
- **Governance boundary:** admin operations audited

---

## 6. Q&A Permissions (concept)

- Authenticated users may ask/answer (`QA_ASK`, `QA_ANSWER`)
- Mark useful: authenticated, not on own answer (`QA_MARK_USEFUL`)
- Accept answer: asker of that question, or moderator (`CONTENT_MODERATE`). `QA_ACCEPT_ANSWER` is not granted globally (open decision #13 resolved for MVP).
- Report: authenticated `CONTENT_REPORT` (anonymous reports: no)
- No private DM fallback

---

## 7. Non-Permissions (MVP)

- Paywall bypass concepts (no paywall)
- AI authority overrides of curated content
- Contributor-created root taxonomy
- Username/password identity administration
