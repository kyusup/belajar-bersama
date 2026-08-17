# Security Policy — Belajar Bersama

## Supported versions

This project is pre-production. Treat the current `main`/default branch as the only line that receives security fixes.

## Reporting a vulnerability

Do **not** open a public issue for a security vulnerability.

If the repository host supports private vulnerability reporting (for example GitHub Security Advisories), use that.

Otherwise contact the maintainers through a private channel once one is published for the project. Do not include session cookies, OAuth secrets, or personal data in public discussion.

## Please include

- Affected endpoint or page
- Impact (for example IDOR, self-approval, score manipulation, private data exposure)
- Steps that do **not** require publishing exploit payloads to a public tracker

## Baseline

Authentication, authorization, object ownership, and quiz scoring are enforced on the API. Frontend checks are UX only. See [docs/SECURITY_ARCHITECTURE.md](docs/SECURITY_ARCHITECTURE.md).
