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
- [x] `frontend/` exists (React + Vite + TS + Tailwind).
- [x] UI shows accounts, transactions, and net worth.
- [x] UI shows at least 1–2 peace metrics.
- [x] Calm defaults: no noisy alerts, readable spacing/typography.
- [x] Frontend build succeeds (`npm -C frontend run build`).

## Assumptions / defaults (explicit)
- Single-user local MVP (no auth).
- Backend serves on `localhost:8080` during dev.

## Suggested follow-up issues (next milestones)
- Add auth + multi-user data separation.
- Add richer visualization + goal tracking UI.

## Task notes
- Slice 3a/3b implementation uses a tab-based single-screen layout (`Dashboard` + `Accounts`) instead of route-based pages to keep MVP complexity low.
- Route-based navigation can be introduced in the Account detail slice once URL-deep-link behavior is needed.
- Slice 3c adds account detail behavior inside the `Accounts` tab: account selection + transactions list + simple filters (`direction`, `memo contains`).
- Follow-up fix: accounts loading loop resolved by removing self-canceling effect dependency; accounts reloads are now driven by explicit reload tick events.
- Follow-up fix: `Create account` form added in Accounts view (name + currency + type) and wired to `POST /accounts`.
- Next slice: `Create transaction` form added in Account detail (date + direction + amount + memo) and wired to `POST /accounts/{accountId}/transactions`, with immediate list refresh.
- Next slice: UI localized to Russian and navigation deep-linking added via URL query params (`tab`, `accountId`) so the selected view/account survives refresh and is shareable.
- Next slice: CSV import form added in Account detail and wired to `POST /imports/transactions/csv` (multipart), including import summary (`received/imported/skipped`) and automatic refresh of transactions + account balances.
