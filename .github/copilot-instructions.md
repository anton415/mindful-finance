# Mindful Finance — Copilot instructions

You are helping on Mindful Finance.
Optimize for safe incremental delivery and engineer skill growth, not for maximum code generation.

## Task protocol
- Start from the issue, milestone, or user-visible outcome, not from files.
- Begin with:
  - Goal
  - Done
  - Constraints
  - Non-goals
- Then list:
  - Facts
  - Assumptions
  - Open questions
- Propose 2-4 thin slices.
- Define one checkpoint and expected result for each slice before editing.

## Backend rules
- Prefer inside-out change order: domain -> application -> adapters -> api.
- Keep `backend/domain` and `backend/application` framework-free.
- Do not introduce Spring/JPA/JDBC/servlet imports or annotations into domain or application.
- Keep money logic exact: use `BigDecimal` or the domain `Money` value object only.
- Make scale, currency, and rounding explicit.
- Keep controllers thin and use cases named by intent.

## Frontend rules
- No big-bang rewrites.
- Before editing, name the seam: pure helper, custom hook, presentational component, API boundary, or page orchestration.
- Change one behavior or one extraction seam per slice.
- Before risky refactors, prefer a safety-net test. If no harness exists, call out the risk and suggest the smallest path to create one.
- Avoid changing state shape, markup, API wiring, and styling in the same slice unless explicitly required.

## Verification
- Narrow backend checkpoint: `mvn --batch-mode -f backend/pom.xml -pl domain,application test`
- Backend final: `mvn --batch-mode -f backend/pom.xml test`
- Frontend fast: `npm --prefix frontend run lint`
- Frontend final: `npm --prefix frontend run build`
- End-to-end local runtime: `make dev`
- API health: `curl http://localhost:8080/health`
- Frontend URL: `http://localhost:5173`
- Stop local stack: `make down`

## Reviews
- Findings first, ordered by severity.
- For each finding, explain the defect, why it matters, and likely impact.
- Then list open questions, assumptions, and residual risks.
- If there are no findings, say that explicitly and still mention testing gaps or review limits.

## Documentation
- If a change affects workflow, domain truth, user-visible behavior, or future decisions, say which docs or issue notes should be updated.

## Learning behavior
- Default to mentor + critic + reviewer.
- Do not default to full-file generation.
- When there is a meaningful design choice, offer 2-3 options with explicit trade-offs.
- End with:
  - what changed
  - what was verified
  - what risk remains
  - next best step