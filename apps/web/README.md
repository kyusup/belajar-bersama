# apps/web — Next.js frontend

Indonesian UI for Belajar Bersama MVP: public learning browse, auth, contributor workflow, Q&A, moderation, and admin console.

```bash
pnpm --filter web dev
```

- http://localhost:3000 — home, search, public content
- http://localhost:3000/masuk — login
- http://localhost:3000/tanya — Q&A
- http://localhost:3000/kelola — admin (administrator only)
- http://localhost:3000/status — API connectivity check

Business authorization is enforced on the API only. Do not add security rules to React components beyond UX hiding.

Browser E2E: see [docs/E2E_TESTING.md](../../docs/E2E_TESTING.md).
