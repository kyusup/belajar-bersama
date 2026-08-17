# Governance — Belajar Bersama

Defines platform governance roles and responsibilities.  
Content-specific workflow details live in `CONTENT_GOVERNANCE.md`, `VERIFICATION.md`, and `MAKER_CHECKER.md`.

---

## 1. Purpose of Governance

Governance exists to protect:

- educational quality
- competency-scoped contribution
- fair and auditable review
- community safety and moderation
- taxonomy integrity
- privacy and copyright respect

Governance is not a substitute for domain enforcement. Critical rules (especially no self-approval and scoped verification) must eventually be enforced in software.

---

## 2. Conceptual Roles

A user may hold multiple roles. Role assignment does not erase maker–checker separation for a single submission.

### Learner

- Consumes public learning content
- May use authenticated learning features (progress, bookmarks, quiz history)
- May participate in permitted Q&A activities
- Cannot publish educational content

### Verified Contributor / Maker

- Authenticated user verified for one or more competencies
- May create and revise educational content within verified competency scope
- Submits content for review
- Cannot approve their own submission
- Cannot expand verification scope by self-assertion

### Checker

- Reviews eligible submissions for competencies/domains they are authorized to check
- May approve, request changes, or otherwise record review decisions per workflow
- Must be eligible for the relevant competency/domain
- Must not review as sole approver on their own maker submission

### Moderator

- Handles community/Q&A/content reports and moderation actions within policy
- Does not replace checker review for educational publication quality
- Exact escalation paths: `OPEN DECISION` where not yet defined

### Administrator

- Manages verification authority (initial model)
- Maintains governed taxonomy (subjects, categories, education levels, domains, competencies)
- Manages users/roles and platform configuration
- Oversees governance integrity

---

## 3. Initial Verification Authority

Initial verification of contributors is administered by the platform Administrator / governance role.

Future verification mechanisms may incorporate credentials, education, professional experience, institutional affiliation, and other evidence.  
Those mechanisms are not required for MVP UI, but the domain must allow them later. See `VERIFICATION.md`.

---

## 4. Taxonomy Governance

Administrators/governance maintain:

- subjects
- categories
- education levels
- domains
- competencies

Contributors select from governed taxonomy; they do not freely proliferate top-level taxonomy.

---

## 5. Separation of Duties

| Rule | Requirement |
|---|---|
| Maker ≠ approver | Maker cannot approve own submission |
| Checker eligibility | Checker must be eligible for relevant competency/domain |
| Role stacking | Allowed globally; forbidden as self-approval on same submission |
| Admin override | `OPEN DECISION` — whether and how admin may force-publish is unresolved |

---

## 6. Accountability

Governance actions that affect verification, publication, moderation, or role grants should be auditable.

Audit detail level for each action type is refined in the domain model (`AuditEvent`).

---

## 7. Funding and Commercial Neutrality (Governance Stance)

Governance must not introduce monetized access to public learning content in MVP.  
Commercial features require explicit future product decisions and constitution alignment.
