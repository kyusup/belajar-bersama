# Privacy Principles — Belajar Bersama

Privacy-by-default principles for a platform that may be used by minors and the general public.

---

## 1. Default Stance

Collect, store, and expose only personal data necessary for:

- authentication
- learning personalization features the user opts into by signing in
- contribution and review workflows
- safety, moderation, and abuse prevention

If data is not needed for those purposes, do not collect it in MVP design.

---

## 2. Public Exposure Rules

Do not expose publicly:

- email addresses
- authentication provider details beyond what is necessary for the signed-in user themselves
- unnecessary personal data

Public profiles, if any, should use minimal identity (for example display name) and avoid sensitive attributes.

Whether public contributor profiles exist in MVP and what fields they show: `OPEN DECISION`.

---

## 3. Authentication Data

Initial auth methods: Google Sign-In, Apple Sign-In.

Identity linkage belongs in private identity records, not in public learning or Q&A payloads.

---

## 4. Minors

Assume minors may use the platform for learning.

Design implications:

- minimize personal data
- avoid private channels that increase risk
- keep moderation/report paths available for public Q&A and content

Age-gate / parental consent implementation details: `OPEN DECISION` (legal/product).

---

## 5. Messaging

MVP must **not** include private direct messaging between users.

Q&A is public and moderated, not a private communication substitute.

---

## 6. Learning Data

Authenticated learning data (progress, bookmarks, quiz history) is personal.

It must not be exposed publicly by default.

Sharing/export of learning data: `OPEN DECISION`.

---

## 7. Moderation and Safety vs Privacy

Moderation may require authorized access to report context and limited user identifiers.  
That access is privileged, auditable, and not a reason to broaden public data exposure.

---

## 8. Non-Goals (MVP)

- advertising profiles / tracking for ads
- sale of personal data
- private DM social graph
- unnecessary government ID collection for normal learning
