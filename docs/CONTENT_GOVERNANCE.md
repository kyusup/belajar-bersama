# Content Governance — Belajar Bersama

Defines who may create educational content and how content becomes public.

---

## 1. Core Rule

Only **Verified Contributors (Makers)** may create educational content, and only within competencies for which they are verified.

A normal authenticated learner cannot directly publish educational material.

---

## 2. Governance Pipeline

```text
Learner
  → Verification (competency-scoped)
    → Verified Contributor / Maker
      → Create Draft
        → Submit for Review
          → Checker (eligible)
            → Approve OR Request Changes
              → Published
```

Publication without successful review is not allowed for educational content types covered by this policy.

---

## 3. What Counts as Educational Content

At minimum, content governance applies to publishable learning materials such as:

- Learning paths
- Courses
- Modules
- Lessons
- Educational materials attached to lessons/courses
- Quizzes and their scored/learning questions intended as curriculum

Q&A posts are community learning artifacts with moderation, not the same as curriculum publication.  
They still require authenticity, safety, and reportability controls.

Exact boundary between “curriculum content” and “community learning artifacts”: partially `OPEN DECISION` for edge cases (for example user-created practice sets).

---

## 4. Maker Obligations

Makers must:

- create within verified competency scope
- provide required attribution/source/license metadata where applicable
- submit for review rather than self-publish
- revise after changes-requested feedback as the normal correction path

Makers must not:

- approve their own submission
- claim verification outside granted competencies
- invent uncontrolled top-level taxonomy entries

---

## 5. Checker Obligations

Checkers must:

- be eligible for the relevant competency/domain
- review against quality, scope, attribution/license readiness, and applicable safety expectations
- record decisions and comments
- prefer requesting changes over silently overwriting maker work

Checkers must not:

- approve a submission where they are the maker
- approve outside eligibility

---

## 6. Taxonomy Selection

Contributors select governed taxonomy entries (subject, education level, domain/competency, and related classifications) when creating content.  
Taxonomy maintenance is an Administrator/governance responsibility.

---

## 7. Copyright and Safety Gates

Before publication, review should confirm that required copyright/attribution fields are present enough to proceed, and that obvious policy violations are not ignored.

Full legal takedown automation and full sensitivity workflows are not MVP requirements, but publication must not strip the ability to represent:

- license / attribution / source
- report status
- future sensitivity/disclaimer metadata

---

## 8. Enforcement Level

Human procedure is insufficient alone.  
Domain and application layers must eventually enforce:

- verification required to create educational content
- competency scope checks
- maker cannot approve own submission
- publication only from allowed lifecycle transitions
