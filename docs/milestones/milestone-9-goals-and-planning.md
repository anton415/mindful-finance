# Milestone 9 — Goals & Planning (v1)

## Goal / Done definition
- **Goal:** turn the existing LifeGoal domain concept into a user-visible planning flow.
- **Done when:**
  - A user can create, edit, archive, and review goals from the UI.
  - Goal progress is computed from current truth data and shown calmly.
  - The system shows at least one planning signal, such as required monthly contribution or ETA.
  - Goals appear in dashboard-level product flow, not as a disconnected side screen.

## Summary (scope)
- **In scope:** goal CRUD, progress calculations, dashboard integration, goal list/detail UI, basic planning signals.
- **Out of scope:** brokerage projections, market return simulation, shared family plans, notifications.

## Facts
- The domain already contains `LifeGoal` and related value types.
- The current UI does not expose goals yet.
- Calm product direction favors long-term planning over noisy short-term updates.

## Assumptions / defaults (explicit)
- v1 goals use the existing `LifeGoal` shape: title, target amount, target date, status, notes.
- Goal progress is derived from current balances/aggregates rather than manual percentage input.
- v1 supports a small set of statuses only (`ACTIVE`, `ACHIEVED`, `ARCHIVED`).

## Open questions
- Should progress map to total net worth in the same currency, or to a narrower subset of assets later?
- Do we need one default planning signal or two (`monthly contribution`, `eta`) in the first slice?

## Step-by-step implementation plan (with checkpoints)
1) **Expose goal management contracts**
- Add repositories/use cases/API contracts for goal create/list/update/archive behavior.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Add progress + planning calculations**
- Compute current progress and at least one planning signal from Truth data.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Build goal UI**
- Add goal list, detail/edit form, and calm status presentation in frontend.
- Checkpoint: `npm -C frontend run build`

4) **Integrate into dashboard**
- Surface goals and planning signals in a way that supports long-term review.
- Checkpoint: `npm -C frontend run build`

5) **Document behavior**
- Capture how progress is derived and what is intentionally excluded in v1.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] User can create and edit a financial goal.
- [ ] User can archive or mark a goal achieved.
- [ ] Dashboard shows goal progress and at least one planning signal.
- [ ] API and persistence support goal management.
- [ ] Frontend build passes (`npm -C frontend run build`).
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).

## Suggested follow-up issues (next milestones)
- Add goal funding plans linked to recurring templates.
- Add richer visualization of progress over time.
- Add scenario planning for multiple contribution strategies.

## Task notes
- Reuse the existing `LifeGoal` domain foundation instead of inventing a parallel goal model.
- Keep planning formulas explicit and explainable.
- Annual `Personal Finance` manual review keeps one shared truth-ledger: each personal-finance card owns a linked `CASH` account, baseline amount is represented as a synthetic ledger transaction, and monthly actual income/expense upserts synthetic account transactions instead of introducing a second ledger.
- Recurring limits and recurring income template are card-scoped non-versioned defaults in `v1`; they repeat across months and years for review purposes, while only actual income/expense changes the linked account balance and Peace metrics.
- Decision (2026-03-14): renaming a personal-finance card also renames its linked `CASH` account so `Личные финансы` and `Инвестиции` stay consistent.
