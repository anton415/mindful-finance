# Milestone 12 — Single-Owner Remote Access via Home Server + Cloudflare (v1)

## Goal / Done definition
- **Goal:** make the current local MVP reachable over HTTPS from anywhere for one owner through a home server and Cloudflare, without turning the product into a multi-user SaaS yet.
- **Done when:**
  - The app is reachable over HTTPS on `app.<domain>` through Cloudflare Tunnel.
  - Access is protected by Cloudflare Access `One-time PIN` for one allowlisted email.
  - A repeatable deployment path exists for `frontend`, `backend`, `PostgreSQL`, `nginx`, and `cloudflared` on one Linux host.
  - The home server does not expose inbound `80/443/5432` to the public Internet.
  - Backup, restore, and smoke-check steps are documented and manually validated.

## Summary (scope)
- **In scope:** hybrid single-owner deployment, home-server baseline, production config, Cloudflare Tunnel, Cloudflare Access, off-device backups, restore drill, and operational smoke checks.
- **Out of scope:** multi-user auth, invite flow, login screen inside the app, public signup, managed cloud database, staging, Kubernetes, advanced observability, and non-essential product features from later milestones.

## Facts
- The current project is a local MVP: Spring Boot backend, PostgreSQL persistence, React/Vite frontend.
- The current local database setup already runs in Docker for development.
- Frontend traffic already assumes a same-origin `/api` contract through the Vite proxy.
- Backend tests and frontend production build currently pass.
- Existing CRUD v1 work for accounts and transactions is still tracked separately in Milestone 7.

## Assumptions / defaults (explicit)
- The next release target is **single-owner remote access**, not a closed beta for multiple invited users.
- The production topology starts with one always-on Linux host running `Docker Compose`.
- The host runs `postgres`, `backend`, `nginx`, and `cloudflared`; PostgreSQL stays private inside the compose network.
- Cloudflare Access protects the hostname at the edge; this milestone does **not** add `POST /auth/login`, `POST /auth/logout`, or `GET /auth/session` to the app.
- Backups use `restic` to `Backblaze B2` by default unless a later decision replaces only the storage target.

## Open questions
- Should the first restore drill run on the same host with clean volumes, or on a separate disposable machine?
- Do we want a host-level firewall baseline documented in this milestone, or is `no port forwarding + tunnel only` enough for v1?
- Should local single-user data be migrated into the home-server deployment, or should the first remote instance start from a clean database?

## Step-by-step implementation plan (with checkpoints)
1) **Lock the hybrid brief**
- Confirm that `home server + Cloudflare Tunnel + Cloudflare Access` is the target deployment shape.
- Capture prerequisites from existing milestones that must be closed before real usage.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

2) **Prepare the home-server baseline**
- Choose and prepare an always-on Linux host with Docker Engine and Compose.
- Define persistent storage for PostgreSQL and app runtime data.
- Keep router port forwarding disabled and avoid exposing backend or database ports publicly.
- Checkpoint: host bootstrap checklist completed and services survive a reboot.

3) **Prepare the production delivery path**
- Build the frontend as static assets and serve it behind `nginx`.
- Reverse-proxy `/api` from `nginx` to Spring Boot and keep the public contract same-origin.
- Externalize runtime secrets and production config so dev defaults are not reused.
- Checkpoint: `curl http://127.0.0.1:8081/api/health`

4) **Publish through Cloudflare Tunnel**
- Create a named tunnel and run `cloudflared` on the home server.
- Point `app.<domain>` to the tunnel instead of opening inbound ports.
- Keep origin access local to the host or compose network only.
- Checkpoint: `curl https://app.<domain>/api/health`

5) **Protect the app with Cloudflare Access**
- Create a self-hosted Access application before publishing the hostname broadly.
- Use `One-time PIN` and an allow policy limited to one owner email.
- Treat Access as the only auth boundary for this milestone; do not add login UI or app auth endpoints.
- Checkpoint: allowlisted email reaches the app after OTP, non-allowlisted email does not.

6) **Operational safety**
- Define deploy, backup, restore, and manual smoke-check steps.
- Run at least one restore drill against clean volumes and record the result.
- Keep smoke validation focused on health, dashboard read, and one write path.
- Checkpoint: manual restore drill and smoke checklist both pass.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] Public HTTPS domain is available for the app through Cloudflare Tunnel.
- [ ] Only the allowlisted owner email can reach the app after Cloudflare Access OTP.
- [ ] Frontend, backend, PostgreSQL, `nginx`, and `cloudflared` can be deployed via a repeatable documented flow on one host.
- [ ] The home-server public IP does not expose inbound `80/443/5432`.
- [ ] Backup and restore steps are documented and manually validated.
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).
- [ ] Frontend build passes (`npm -C frontend run build`).

## Suggested follow-up issues (next milestones)
- Add app-level authentication and user isolation only when multi-user or invited beta becomes a real requirement.
- Add observability and alerting beyond basic smoke checks.
- Add staging or managed-cloud migration only after the single-owner remote-access path is stable.

## Task notes
- Treat this as a private remote-access milestone, not as a reason to collapse domain boundaries.
- Keep money logic and calculation rules inside the existing domain/application boundaries.
- Prefer a tight perimeter and operational clarity over early platform complexity.
