# Milestone 12 — Closed Beta Launch on Yandex Cloud (v1)

## Goal / Done definition
- **Goal:** make the current local MVP usable by a small invited group through Yandex Cloud without turning the product into a full public SaaS yet.
- **Done when:**
  - The app is reachable over HTTPS on a public domain.
  - Authentication is required for all real user data.
  - Each user can access only their own accounts, transactions, and derived metrics.
  - A repeatable deployment path exists for frontend, backend, and PostgreSQL.
  - Backup and smoke-check steps are documented for beta operations.

## Summary (scope)
- **In scope:** closed beta launch, Yandex Cloud deployment baseline, authentication, user data isolation, production config, and operational safety basics.
- **Out of scope:** open signup, OAuth/social auth, mobile app packaging, Kubernetes, advanced observability stack, and non-essential product features from later milestones.

## Facts
- The current project is a local MVP: Spring Boot backend, PostgreSQL persistence, React/Vite frontend.
- The current roadmap explicitly treated auth and multi-user support as out of scope for the MVP.
- Backend tests and frontend production build currently pass.
- Existing CRUD v1 work for accounts and transactions is still tracked separately in Milestone 7.

## Assumptions / defaults (explicit)
- First public release is a **closed beta**, not an open signup launch.
- Yandex Cloud starts with a pragmatic setup: one VM for app delivery and a managed PostgreSQL instance for user data.
- v1 authentication uses email + password with server-side sessions/cookies, not JWT or third-party identity providers.
- A bootstrap admin user can be created from environment variables for initial beta operations.

## Open questions
- Should invited beta users be created only by admin, or should invite links be added later as a follow-up?
- Should local single-user data be migrated into the bootstrap admin account automatically, or should beta start from a clean database?
- Do we want a staging environment before the first production beta, or is a single production-like environment enough for v1?

## Step-by-step implementation plan (with checkpoints)
1) **Lock the beta launch brief**
- Confirm that closed beta on Yandex Cloud is the target and document the chosen deployment shape.
- Capture prerequisites from existing milestones that must be closed before user invite.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

2) **Add auth + user ownership in backend**
- Introduce users, password hashing, session-based auth, and ownership boundaries for accounts/transactions/metrics.
- Protect all existing HTTP API endpoints so data is always scoped to the current user.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add auth flow in frontend**
- Add login/logout, auth bootstrap, and protected UI shell behavior.
- Keep the current Calm UI structure, but block access for anonymous users.
- Checkpoint: `npm -C frontend run build`

4) **Prepare production delivery path**
- Serve the built frontend via a public web server and reverse-proxy API requests to Spring Boot.
- Externalize secrets and production config so the local dev defaults are not reused in beta.
- Checkpoint: local or staging smoke run with the production-like topology

5) **Provision Yandex Cloud baseline**
- Create networking, VM, managed PostgreSQL, DNS, TLS, and deploy/runtime configuration.
- Restrict database and backend access to the minimum required network paths.
- Checkpoint: `curl https://<beta-domain>/api/health`

6) **Operational safety + beta rollout**
- Define backup, restore, deploy, and smoke-test steps.
- Roll out to a very small invited group first, then expand only after manual verification.
- Checkpoint: manual beta smoke checklist

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] Public HTTPS domain is available for the beta app.
- [ ] Users must authenticate before accessing personal data.
- [ ] User A cannot access User B data through API or UI.
- [ ] Frontend, backend, and database can be deployed via a repeatable documented flow.
- [ ] Backup and restore steps are documented and manually validated.
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).
- [ ] Frontend build passes (`npm -C frontend run build`).

## Suggested follow-up issues (next milestones)
- Add invite-based onboarding or public signup after the closed beta is stable.
- Add observability and alerting beyond basic smoke checks.
- Add staging and deploy automation once the production baseline settles.

## Task notes
- Treat this as a productization milestone, not as a reason to collapse domain boundaries.
- Keep money logic and calculation rules inside the existing domain/application boundaries.
- Avoid overengineering the first cloud launch; operational clarity matters more than platform maximalism.
