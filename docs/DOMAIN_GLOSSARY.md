# Domain Glossary — Belajar Bersama

Canonical terms for product and engineering. Prefer these meanings in docs and code discussions.

| Term | Meaning |
|---|---|
| Belajar Bersama | Product name; open learning platform and knowledge community for Indonesian society |
| Anonymous user | Unauthenticated visitor; may consume public published learning content |
| Authenticated user | User signed in via supported identity providers |
| Learner | Role/capability set focused on consuming learning and permitted learning activities |
| Maker / Verified Contributor | User verified for competency scope who may create educational content drafts and submissions |
| Checker | User eligible to review submissions for relevant competency/domain |
| Moderator | Role for community/Q&A/content report handling |
| Administrator | Governance role for verification, taxonomy, roles, configuration |
| Verification | Competency-scoped grant enabling maker capabilities |
| Competency | Governed area of skill/knowledge used for verification and eligibility |
| Subject | Governed primary subject area in taxonomy |
| Education level | Governed formal/informal level classification (not LMS lock-in) |
| Taxonomy | Platform-governed classification set (subjects, categories, levels, domains, competencies) |
| Educational content | Publishable learning artifact (path/course/module/lesson/material/quiz kinds) |
| Draft | Editable unpublished revision state before/without active review completion |
| Content revision | Versioned snapshot of educational content used for review and history |
| Content submission | Maker–checker workflow instance for a revision |
| Review | Checker evaluation of a submission including comments and decision |
| Publish | Transition making approved content publicly learnable |
| Learning path | Structured route through learning content |
| Course / Module / Lesson | Hierarchical learning containers/units |
| Quiz / Question / Question option | Curriculum assessment constructs |
| Learning progress | Authenticated record of learning advancement |
| Bookmark | Authenticated saved reference to content |
| Q&A question / answer | Public moderated learning Q&A entities |
| Accepted answer | Answer marked as best/accepted by the asker or a moderator |
| Content report | Report of problematic or infringing content/Q&A |
| Audit event | Append-only record of governance/security-relevant actions |
| Source / attribution / license | Rights and credit metadata for educational material |
| Identity | External authentication linkage record for a user |
| Public learning | Published educational content readable without authentication |
| Independent learner | Product goal: users can learn effectively without dependency on gated access or AI-as-truth |
| OPEN DECISION | Explicitly unresolved product/architecture choice |

## Naming notes

- Prefer distinguishing **Question** (quiz) from **QAQuestion** (community).
- Prefer **EducationalContent** when referring to the governed publishable aggregate.
- “Domain” in casual product language usually means competency/taxonomy domain, not DDD domain, unless stated.
