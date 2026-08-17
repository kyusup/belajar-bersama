# Domain Model — Belajar Bersama

Conceptual domain model. Phase 5 implements learner experience on top of Phase 4 educational content: quizzes, attempts, progress, bookmarks, and resume. Phase 6 implements public moderated Q&A and a report queue.

Related docs: `DOMAIN_GLOSSARY.md`, `ROLE_PERMISSION_CONCEPT.md`, `CONTENT_LIFECYCLE.md`, `CONTENT_MODEL.md`.

---

## 1. Modeling Approach

- Prefer clear aggregates and invariants over premature table design.
- Merge entities when separation adds ceremony without behavioral value.
- Keep extension points for evidence-based verification, sensitivity, and takedown without building those UIs now.
- Indonesian product language does not require Indonesian type names in code; glossary maps terms.

---

## 2. Entity Decisions (Evaluated Set)

| Candidate | Decision | Reasoning |
|---|---|---|
| User | **Keep** | Core actor for learning, contribution, review, moderation |
| Identity | **Keep** | Separates auth provider linkage from public/persona aspects of User |
| Role | **Keep (assignment)** | Conceptual roles are many-valued; model as role assignments, not a single enum field on User |
| Verification | **Keep** | Competency-scoped; must not collapse into `isVerified` |
| Competency | **Keep** | Verification and checker eligibility scope |
| Subject | **Keep** | Governed top-level learning topic area |
| EducationLevel | **Keep** | Formal/informal level labels without forcing LMS-only structure |
| Category / Domain | **Keep as TaxonomyNode (or Category)** | Avoid uncontrolled proliferation; “domain” in product language maps to governed taxonomy used for competency grouping. See §4 |
| LearningPath | **Keep** | Ordered/structured route across courses/lessons |
| Course | **Keep** | Mid-level learning container |
| Module | **Keep** | Course subdivision |
| Lesson | **Keep** | Learnable unit |
| Content | **Keep (as EducationalContent)** | Publishable educational artifact abstraction; concrete types specialize it |
| ContentRevision | **Keep** | Immutable-enough snapshot/version for review/audit |
| ContentSubmission | **Keep** | Maker–checker workflow instance over a revision |
| Review | **Keep** | Review activity bound to a submission |
| ReviewDecision | **Value object / record inside Review** | Decision outcome is not a freestanding aggregate; keep as structured decision on Review |
| Quiz | **Keep** | Practice/assessment construct |
| Question | **Keep** | Quiz question (curriculum) — distinct from QAQuestion |
| QuestionOption | **Keep** | Option for objective questions |
| LearningProgress | **Keep** | Authenticated progress records |
| Bookmark | **Keep** | Authenticated bookmarks |
| QAQuestion | **Keep** | Community learning question |
| QAAnswer | **Keep** | Community learning answer |
| ContentReport | **Keep** | Report for content/Q&A/copyright/safety |
| AuditEvent | **Keep** | Cross-cutting audit trail |
| Source | **Keep (value/entity as needed)** | Source/reference metadata for attribution |
| License | **Keep** | License catalog + per-content license binding |

### Merged / renamed intentionally

1. **ReviewDecision** → structured fields on `Review` (outcome, rationale, decidedAt, decidedBy). Avoids orphan entity.
2. **Category / Domain** → governed `TaxonomyNode` (type-discriminated: category, domain, etc.) under platform taxonomy, related to Subject/Competency. Prevents parallel uncontrolled trees.
3. **Content** → `EducationalContent` as the publishable aggregate root name in docs (implementation may shorten).
4. **Answer/Explanation** for quizzes → fields/entities under `Question` (explanation text) rather than a separate global Answer entity conflicting with `QAAnswer`.

### Deferred as first-class MVP aggregates (domain-ready hooks only)

- Verification evidence objects (credentials, affiliation, etc.)
- Sensitivity policy objects
- Full takedown case management

These may appear later as related records without changing core aggregates.

---

## 3. Core Aggregates and Responsibilities

### 3.1 Identity & Access

**User**  
Platform person/actor. Holds display-facing profile fields that are safe to use in product surfaces. Does not embed raw auth secrets.

**Identity**  
Links a User to an external auth subject (Google / Apple). Private. One user may have multiple identities over time; exact linking policy is product-sensitive (`OPEN DECISION` for multi-provider merge rules).

**RoleAssignment**  
Grants a conceptual role (Learner is default capability set; Contributor/Maker via verification; Checker; Moderator; Administrator) to a User, optionally scoped.

**Verification**  
Active grant that a User is verified for a **Competency**. Issued by governance/admin initially. Revocable.

**Competency**  
Governed skill/knowledge area used for maker scope and checker eligibility.

### 3.2 Taxonomy (platform-governed)

**Subject**  
Primary subject area (governed).

**EducationLevel**  
Label for formal or informal level (e.g., SMA, professional, general). Not a hardcoded LMS constraint.

**TaxonomyNode**  
Governed nodes for categories/domains and related classification. Maintained by administrators.

Contributors **select** taxonomy; they do not freely create top-level taxonomy.

### 3.3 Learning structure

```text
Subject
  └── LearningPath
        └── Course
              └── Module
                    └── Lesson
                          └── Quiz / Educational materials
```

Relationships may be many-to-many where pedagogically needed (e.g., a Course in multiple paths), but publication still goes through educational content governance.

**LearningPath / Course / Module / Lesson**  
Structural learning entities. Publishable aspects are represented through `EducationalContent` + revisions/submissions, or each structural entity is an `EducationalContent` subtype.  

**Preferred implemented stance:** LearningPath, Course, Module, Lesson, Material, and Quiz are kinds of `EducationalContent`. Structural parent-child is optional `parent_id`. See `LEARNING_MODEL.md`.

### 3.4 Content review

**EducationalContent**  
Aggregate root for a learning artifact identity (stable id across revisions).

**ContentRevision**  
Versioned body + metadata snapshot (title, body/structure refs, taxonomy snapshot, license/attribution snapshot).

**ContentSubmission**  
Workflow instance: maker submits a revision into review.

**Review**  
Checker’s review of a submission, including decision outcome and comments.

Invariant: maker of submission ≠ approving checker.

### 3.5 Assessment (curriculum)

Implemented in Phase 5. See `QUIZ_MODEL.md` and `ASSESSMENT_RULES.md`.

**Quiz**  
`EducationalContent` kind `QUIZ`. Questions belong to `ContentRevision` (`quiz_spec` / `quiz_question` / `quiz_option`).

**Question / QuestionOption**  
Single choice, multiple choice, true/false. Correct flags are not public before submission.

**QuizAttempt**  
Per-user attempt pinned to the exact quiz revision. Server-calculated score. Immutable after submit.

### 3.6 Learner state (authenticated)

**LearningProgress**  
Computed from lesson completions and required quiz results. Not a stored percentage. See `PROGRESS_MODEL.md`.

**Bookmark**  
Per-user saved references to published content. See `BOOKMARK_MODEL.md`.

### 3.7 Q&A (learning knowledge, not social network)

**QAQuestion** / **QAAnswer**  
Public moderated learning Q&A. Asker or moderator may mark an accepted answer. Learners may mark another person’s answer useful (not reputation). Hidden items are not public. See `QA_MODEL.md`.

### 3.8 Rights, safety, audit

**License**  
License definitions (e.g., CC BY-SA) and binding on revisions.

**Source** (and references)  
Attribution/source/reference records on revisions.

**ContentReport**  
User/moderator reports (copyright, abuse, quality, etc.).

**AuditEvent**  
Append-only record of security/governance-relevant actions.

---

## 4. Relationship Overview

```text
User 1──* Identity
User 1──* RoleAssignment
User 1──* Verification *──1 Competency
Competency *──* TaxonomyNode / Subject (governed association)

EducationalContent *──* Subject / EducationLevel / Competency (via taxonomy bindings)
EducationalContent 1──* ContentRevision
ContentRevision 1──* ContentSubmission
ContentSubmission 1──* Review
ContentSubmission *──1 User (maker)
Review *──1 User (checker)

EducationalContent (Lesson) 1──* Quiz 1──* Question 1──* QuestionOption

User 1──* LearningProgress *──1 EducationalContent (or concept later)
User 1──* Bookmark *──1 EducationalContent

User 1──* QAQuestion 1──* QAAnswer
User 1──* ContentReport

License / Source bound to ContentRevision
AuditEvent references actors + entity ids
```

---

## 5. Value Objects (representative)

- ReviewDecisionOutcome: `APPROVED` | `CHANGES_REQUESTED` | (future: rejected, etc. if adopted)
- ContentLifecycleState: see `CONTENT_LIFECYCLE.md`
- Attribution: author display, attribution text
- SensitivityMark (optional future): low-overhead enum/flag on revision
- ReportReason / ReportStatus

---

## 6. Key Invariants

1. Public read of published educational content does not require authentication.
2. Creating educational content requires active Verification for relevant Competency.
3. Maker cannot approve own ContentSubmission.
4. Approving Checker must be eligible for the submission’s competency/domain.
5. EducationalContent reaches PUBLISHED only via allowed lifecycle transitions after approval.
6. Verification is competency-scoped; no global implied verification.
7. Taxonomy creation at platform level is admin/governance-only.
8. Personal emails and auth identifiers are not part of public content/Q&A projections.
9. MVP has no private DM entity.
10. License/attribution metadata is representable on revisions; third-party work is not auto-relicensed.

---

## 7. Authorization Boundaries

| Boundary | Enforced by |
|---|---|
| Anonymous public learn | published content read APIs |
| Authenticated personalization | LearningProgress, Bookmark, quiz history ownership |
| Contribution | Verification + competency scope |
| Review | Checker eligibility + not maker |
| Moderation | Moderator/Admin on reports |
| Taxonomy/verification grants | Administrator |

Details: `ROLE_PERMISSION_CONCEPT.md`.

---

## 8. Audit Requirements

At minimum, emit AuditEvent for:

- verification grant/revoke
- role assignment changes
- submission submit/resubmit
- review decisions
- publish/archive transitions
- moderation actions on reports
- license/takedown status changes (when introduced)

---

## 9. Explicit Non-Goals of This Model Version

- AI tutoring entities as source of truth
- Payment/subscription entities
- Private messaging
- Full adaptive mastery graph (leave room via future Concept/Mastery records)
- Full verification-evidence workflow UI
