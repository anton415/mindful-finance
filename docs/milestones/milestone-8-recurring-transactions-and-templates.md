# Milestone 8 — Recurring Transactions & Templates (v1)

## Goal / Done definition
- **Goal:** reduce repetitive manual entry by letting a user define recurring money flows once and reuse them safely.
- **Done when:**
  - A user can create recurring transaction templates.
  - The system can preview upcoming planned transactions from those templates.
  - Templates can be paused/resumed without data loss.
  - The frontend exposes a calm management flow for recurring rules.

## Summary (scope)
- **In scope:** recurring template model, schedule calculation, API + PostgreSQL persistence, UI management, planned-occurrence preview.
- **Out of scope:** bank sync, background job infrastructure, variable-amount automation, shared household templates.

## Facts
- The current product supports manual transaction creation and CSV import, but not recurring flows.
- Milestone 7 adds safe editing/deletion, which reduces the risk of correcting generated entries later.

## Assumptions / defaults (explicit)
- v1 supports fixed-amount templates only.
- v1 supports weekly and monthly schedules.
- Generated occurrences stay in preview/planned state until explicitly materialized by the user or an application flow.

## Open questions
- Should materialization happen on-demand only in v1, or automatically when a scheduled date arrives?
- Should recurring rules allow both inflow and outflow templates from day one?

## Step-by-step implementation plan (with checkpoints)
1) **Add recurring template model**
- Define template fields, frequency rules, and persistence shape.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Create template management flows**
- Add create/list/update/pause/resume behavior in application, API, and persistence.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add planned occurrence preview**
- Generate upcoming occurrences for a selected horizon (for example, 30/60/90 days).
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Expose Calm UI**
- Add UI to manage templates and inspect upcoming planned transactions.
- Checkpoint: `npm -C frontend run build`

5) **Document constraints**
- Capture supported schedule rules and any non-goals in docs.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] User can create a recurring transaction template.
- [ ] User can view upcoming planned transactions generated from templates.
- [ ] User can pause and resume a recurring template.
- [ ] API and PostgreSQL persistence support recurring template management.
- [ ] Frontend build passes (`npm -C frontend run build`).
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).

## Suggested follow-up issues (next milestones)
- Connect recurring templates to goals and planned contributions.
- Add exceptions/skip-once behavior for a single occurrence.
- Add automatic materialization via scheduled jobs.

## Task notes
- Keep recurring schedule logic framework-free inside the application/domain boundary.
- Prefer explicit user confirmation over silent auto-posting in v1.
