# Open Decisions — Belajar Bersama

Genuinely unresolved decisions. Do not treat these as requirements.

If a decision is made later, resolve it here and update the affected docs in the same change.

---

## Product / Governance

1. **Admin force-publish**  
   May an Administrator publish without normal checker approval in emergencies? If yes, under what audit constraints?

2. **Archive authority** — **Resolved in Phase 4 for MVP.** Maker or administrator with `CONTENT_ARCHIVE`. Soft archive only.

3. **Auto-publish vs explicit publish** — **Resolved in Phase 4.** `APPROVED` does not become public. An explicit publish operation is required.

4. **Boundary: curriculum vs community artifacts**  
   Which edge artifacts (e.g., user practice sets) require full maker–checker publication vs lighter moderation?

5. **Verification evidence requirements**  
   What evidence, if any, is required before admin grants competency verification in the first operational version?

6. **Verification revocation & appeals**  
   Policy for suspend/revoke and whether appeals exist.

7. **Checker eligibility model** — **Resolved in Phase 3.**  
   Checker eligibility = stored `CHECKER` role **and** an `APPROVED` verification for the relevant competency **and** the checker is not the maker of the submission. See `AUTHORIZATION_POLICIES.md`.

8. **Moderator escalation paths**  
   Exact handoff between moderator actions and checker/admin for educational content disputes.

---

## Privacy / Trust & Safety

9. **Public contributor profiles**  
   Whether MVP exposes public profiles and which fields are visible.

10. **Age-gate / parental consent**  
    Legal/product approach for minors beyond privacy-by-default data minimization.

11. **Anonymous content reports** — **Resolved in Phase 4 for MVP.** Reports require an authenticated user with `CONTENT_REPORT`. Anonymous reports are not accepted.

12. **Learning data export/sharing**  
    Whether users can export or share progress/bookmarks.

13. **Q&A accepted-answer authority** — **Resolved in Phase 6 for MVP.** Hybrid: the asker may accept/unaccept on their own question; a moderator with `CONTENT_MODERATE` may also accept/unaccept. `QA_ACCEPT_ANSWER` stays ungranted as a global capability.

---

## Copyright / Licensing Operations

14. **Minimum mandatory rights metadata at submit/publish** — **Resolved in Phase 4 for MVP.** Submit requires title, non-empty body, subject, education level, at least one competency, and a license. Sources are optional.

15. **Default license enforcement**  
    Whether CC BY-SA is mandatory for original contributor content or only recommended default.

---

## Identity / Platform

16. **Multi-provider identity merge**  
    Rules when the same person signs in with Google and Apple. Phase 3 does **not** auto-merge on email. Linking must be deliberate.

17. **Display name policy**  
    Generation, uniqueness, change limits, and moderation of display names.

---

## Learning Domain

18. **Many-to-many structural graph**  
    How freely courses/lessons may appear in multiple paths in MVP vs stricter tree. **Phase 5 keeps a single `parent_id` tree.**

19. **Concept-level mastery model**  
    When and how Concept/Mastery entities are introduced beyond content-unit progress. **Phase 5 computes content-unit progress only.**

23. **Quiz randomization** — **Resolved in Phase 5 for MVP.** Configured `sort_order` only. Random delivery would need attempt-level snapshots of presented order.

24. **Default passing score** — **Resolved in Phase 5.** If `passingScore` is null, do not invent one; `passed` stays null.

25. **Open-to-complete lessons** — **Resolved in Phase 5.** Completion is explicit (“Tandai sudah selesai”), not implied by opening.

20. **Assignment of checkers** — **Resolved in Phase 4 for MVP.** Hybrid: administrator may assign an eligible checker; eligible checkers may claim `SUBMITTED` items. Makers cannot choose the checker.

---

## Architecture (resolved in Phase 2)

21. **Monorepo tooling** — **Resolved** in [ADR-009](adr/ADR-009-monorepo-tooling.md): pnpm workspaces + Maven single-module Quarkus with package layers.

22. **API style between web and api** — **Resolved** in [ADR-010](adr/ADR-010-api-contract-style.md): REST with code-first OpenAPI.

26. **Rate-limit backend** — **Resolved in Phase 7 for MVP.** Quarkus in-process filter, not an external gateway. Shared-store limiting is future work for multi-instance production.

These are recorded so implementation does not silently invent product policy.

