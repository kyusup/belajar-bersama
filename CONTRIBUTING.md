# Contributing — Belajar Bersama

Thank you for considering a contribution.

## Product rules

Read [docs/PRODUCT_CONSTITUTION.md](docs/PRODUCT_CONSTITUTION.md) first.

Do not add:

- paywalls, ads, subscriptions, or engagement gamification
- username/password authentication
- private direct messaging
- AI tutors as an authority over reviewed content
- public email addresses or unnecessary personal data

Educational content is quality-first and maker–checker reviewed. Q&A is moderated community learning, not a social network.

## How to run

See [docs/DEVELOPMENT_SETUP.md](docs/DEVELOPMENT_SETUP.md).

## How to change code

1. Domain and authorization live in the API. Do not rely on the frontend for security.
2. State transitions use explicit operations, not generic PATCH of status fields.
3. Add tests for new business rules.
4. Update the relevant document under `docs/` so it describes actual behavior.
5. Keep software license (Apache-2.0) separate from educational-content licenses.

## Governance contributions

Verification, roles, taxonomy, and moderation are administrator/moderator capabilities. Contributors cannot grant themselves privileged roles.

## Questions

Open decisions that would change product, privacy, legal, or security behavior belong in [docs/OPEN_DECISIONS.md](docs/OPEN_DECISIONS.md) — do not silently invent policy.
