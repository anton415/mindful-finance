# Milestone 11 — Russian FIRE Engine & Backend Refactor (v1)

## Goal / Done definition
- **Goal:** refactor the Spring Boot backend so FIRE/Peace calculations become explicit, testable, and extensible for Russian investment/tax realities.
- **Done when:**
  - Calculator boundaries in backend are clearer than the current ad-hoc `peace/*` shape.
  - FIRE calculators support Russian-specific rules needed for v1 analysis: IIS A/B, NDFL 13/15, OFZ coupon flows, and inflation inputs.
  - Calculator logic is covered by focused JUnit 5 unit tests.
  - API contracts and docs describe the supported assumptions and limitations.

## Summary (scope)
- **In scope:** Spring Boot backend refactor around calculator boundaries, new application/use-case layer for FIRE metrics, Russian tax/investment rules for v1, data adapters for inflation/coupon inputs, and JUnit 5 coverage for calculation flows.
- **Out of scope:** brokerage integrations, market-price forecasting, production-grade tax advisory, multi-country tax models, and frontend redesign.

## Facts
- The current backend already exposes basic Peace metrics such as monthly burn and monthly savings.
- JUnit 5 is already used in the repository, so this milestone extends coverage rather than introducing a new test stack.
- The current product roadmap does not yet contain a dedicated backend milestone for FIRE-specific modeling beyond the first Peace metrics.

## Assumptions / defaults (explicit)
- v1 models IIS type A and type B as explicit calculation modes with documented assumptions.
- v1 treats NDFL 13/15 as a ruleset for calculator scenarios, not as a full legal/tax engine.
- OFZ support focuses on coupon cash flow handling and integration into FIRE calculations, not full bond pricing analytics.
- Inflation input can start from a deterministic adapter or imported series before any live automatic sync is introduced.

## Open questions
- Which FIRE outputs are most important for v1: safe withdrawal planning, coast FIRE, lean/fat FIRE, or a smaller initial subset?
- Should tax/inflation inputs be stored as versioned snapshots, or computed only from the latest imported dataset?
- How much of the current `peace/*` API should be preserved for backward compatibility versus replaced by a dedicated FIRE namespace?

## Step-by-step implementation plan (with checkpoints)
1) **Refactor calculator boundaries**
- Separate FIRE calculation use cases from generic account/transaction CRUD flows.
- Make the Spring Boot adapter thinner and move calculation rules into application/domain-style modules.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Add Russian tax rules layer**
- Introduce explicit calculation components for IIS A/B and NDFL 13/15 assumptions.
- Document the formulas and the inputs they require.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add market/economic inputs**
- Support OFZ coupon cash flows and inflation-series inputs for FIRE scenarios.
- Keep data fetching/parsing at the edge and calculation logic framework-free.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Expand FIRE calculator outputs**
- Add or refactor calculator use cases so they can consume the new tax/economic inputs.
- Expose clear API contracts for the supported FIRE metrics.
- Checkpoint: `mvn -f backend/pom.xml test`

5) **Strengthen JUnit 5 coverage**
- Add unit tests for formulas, edge cases, and known Russian-specific scenarios.
- Keep integration tests focused on adapter wiring and contract behavior.
- Checkpoint: `mvn -f backend/pom.xml test`

6) **Document assumptions**
- Update milestone notes/API docs with supported formulas, data sources, and non-goals.
- Checkpoint: `mvn -f backend/pom.xml test`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] Backend calculator boundaries are refactored into explicit FIRE-oriented use cases/components.
- [ ] FIRE calculations support IIS A/B scenarios.
- [ ] FIRE calculations support NDFL 13/15 assumptions where applicable.
- [ ] OFZ coupon cash flows and inflation inputs are available to calculators.
- [ ] JUnit 5 unit tests cover core formulas and edge cases.
- [ ] `mvn -f backend/pom.xml test` passes.
- [ ] API/docs describe supported assumptions and known limitations.

## Suggested follow-up issues (next milestones)
- Add versioned economic/tax snapshots for reproducible historical scenarios.
- Add richer FIRE scenario planning in the frontend.
- Add source adapters for official datasets and background refresh workflows.

## Task notes
- Prefer explicit formulas and documented assumptions over “smart” hidden heuristics.
- Treat Russian tax/investment logic as scenario modeling, not legal advice.
- Keep domain/application calculation logic isolated from Spring-specific concerns.
