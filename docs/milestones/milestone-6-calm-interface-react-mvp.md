# Milestone 6 — Calm Interface (React MVP v0)

## Goal / Done definition
- **Goal:** ship a minimal Calm UI that lets a user see Truth + Peace outputs without noise.
- **Done when:**
  - A React + Vite + TypeScript + Tailwind app exists (suggested: `frontend/`).
  - The UI can: list accounts, view transactions, and show net worth + 1–2 peace metrics.
  - Basic UX principles are applied: calm defaults, no flashing alerts, readable typography.

## Summary (scope)
- **In scope:** minimal pages, API integration, and a clean layout.
- **Out of scope:** auth, multi-user, fancy charts, push notifications, mobile app packaging.

## UI slices (v0)
Suggested pages:
- **Dashboard:** net worth (by currency) + 1–2 peace metrics.
- **Accounts:** list accounts + balances.
- **Account detail:** transactions list + simple filters.

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Scaffold frontend**
- Create `frontend/` via Vite (React + TS) and add Tailwind.
- Checkpoint: `npm -C frontend run build`

2) **API client**
- Add a small fetch wrapper and typed DTOs.
- Checkpoint: `npm -C frontend run lint` (if configured)

3) **Implement pages**
- Dashboard, Accounts, Account detail.
- Checkpoint: `npm -C frontend run build`

4) **Connect to backend**
- Proxy `/api` to the Spring Boot app in dev.
- Checkpoint: `npm -C frontend run dev` + browse pages with live backend running.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] `frontend/` exists (React + Vite + TS + Tailwind).
- [ ] UI shows accounts, transactions, and net worth.
- [ ] UI shows at least 1–2 peace metrics.
- [ ] Calm defaults: no noisy alerts, readable spacing/typography.
- [ ] Frontend build succeeds (`npm -C frontend run build`).

## Assumptions / defaults (explicit)
- Single-user local MVP (no auth).
- Backend serves on `localhost:8080` during dev.

## Suggested follow-up issues (next milestones)
- Add auth + multi-user data separation.
- Add richer visualization + goal tracking UI.
