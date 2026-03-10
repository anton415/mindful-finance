# Milestone 14 — Domain Model Alignment & Schema v1 (v1)

## Goal / Done definition
- **Goal:** align the backend domain, persistence schema, and calculation boundaries with `ТЗ-05...09`, so documentation and code describe the same financial model.
- **Done when:**
  - The backend can represent the documented v1 financial operations without overloading everything into generic `direction + amount`.
  - Persistence and application boundaries support the valuation and planning inputs required by the domain docs.
  - Core portfolio/FIRE prerequisite calculations are explicit, testable, and traceable to their inputs.
  - The repo docs and code no longer contradict each other on transaction taxonomy, valuation inputs, or truth/planning boundaries.

## Summary (scope)
- **In scope:** domain type alignment for documented transaction kinds, persistence shape changes, valuation inputs (`PriceSnapshot`), planning inputs (`AnnualExpensesBasis`, `SafeWithdrawalRate`), portfolio/FIRE prerequisite use cases, migration/test coverage, and docs sync.
- **Out of scope:** full Russian tax engine, brokerage sync, derivatives/leverage, full scenario planner UI, and automatic FX/market data ingestion.

## Facts
- The current backend models transactions primarily as `direction + positive Money`, which is not rich enough for the documented `buy/sell/dividend/fee/contribution/withdrawal` taxonomy.
- The current PostgreSQL schema stores `accounts` and generic `transactions`, but does not yet store valuation inputs or FIRE planning inputs.
- `docs/domain/` now defines an `account-ledger first` model with explicit valuation, planning, and derived-state boundaries.

## Assumptions / defaults (explicit)
- `v1` remains `account-ledger first`; `Portfolio`, `Position`, and `CashBalance` are derived, not primary mutable truth.
- `dividend_reinvestment` is modeled as a composite business event (`dividend_cash + buy`), not as a new atomic transaction kind.
- `PriceSnapshot` is a factual valuation input and remains separate from return assumptions or scenario projections.
- `AnnualExpensesBasis` and `SafeWithdrawalRate` are explicit planning inputs; no silent fallback from missing truth to estimate is allowed.
- Scenario persistence can remain out of scope for this milestone, but code boundaries must already separate `fact` from future `plan/scenario` layers.

## Step-by-step implementation plan (with checkpoints)
1) **Align the transaction model with `ТЗ-07`**
- Replace or extend the generic transaction model so the code can represent: `cash_contribution`, `cash_withdrawal`, `buy`, `sell`, `dividend_cash`, and `fee`.
- Add the documented explicit fields needed for these operations, such as `assetId`, `quantity`, `unitPrice`, `grossCashAmount`, `feeAmount`, and `expenseEligibility` where relevant.
- Preserve a clean migration path for existing simple inflow/outflow cash operations.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Align persistence schema with documented domain inputs**
- Add persistence support for `PriceSnapshot`.
- Add persistence support for `AnnualExpensesBasis` and `SafeWithdrawalRate`, including explicit source/basis metadata.
- Keep the schema explicit about what is fact, what is planning input, and what is derived.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add domain/application calculations for documented derived state**
- Implement use cases for `portfolio_state(asOf)`, `cash_balance(asOf)`, `position.quantity(asOf)`, and `portfolio_value(asOf)` by currency.
- Return unresolved valuation state when a required `PriceSnapshot` is missing instead of silently using zero or stale assumptions.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Add FIRE prerequisite calculations from `ТЗ-08`**
- Implement resolution of `AnnualExpensesBasis` for at least `REALIZED_TTM` and `PLANNING_MANUAL`.
- Implement `fire_target` and `progress_to_fire` using explicit `SafeWithdrawalRate` and explicit calculation currency requirements.
- Keep multi-currency behavior truthful: no single total without explicit FX input.
- Checkpoint: `mvn -f backend/pom.xml test`

5) **Separate truth from planning at code boundaries**
- Introduce explicit contracts or namespaces so truth-ledger calculations do not silently consume future planning/scenario inputs.
- Ensure derived metrics can explain which inputs and basis were used.
- Checkpoint: `mvn -f backend/pom.xml test`

6) **Close the docs/code loop**
- Update backend-facing docs, schema notes, and milestone notes to match the implemented shape.
- Add regression tests for the documented control scenarios: `buy + fee`, `sell + fee`, `cash dividend`, `dividend reinvestment`, multi-currency portfolio without FX, and FIRE progress from `planning_annual_expenses + SWR`.
- Checkpoint: `mvn -f backend/pom.xml test`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] Code can represent the documented v1 transaction taxonomy without lossy overloading into generic inflow/outflow semantics.
- [ ] Schema/application boundaries include `PriceSnapshot`, `AnnualExpensesBasis`, and `SafeWithdrawalRate`.
- [ ] Portfolio state/value calculations exist and follow the documented explicit-input rules.
- [ ] FIRE prerequisite calculations (`fire_target`, `progress_to_fire`) exist and require explicit basis and rate inputs.
- [ ] Derived metrics do not silently mix fact and planning/scenario inputs.
- [ ] Regression tests cover the documented control scenarios.
- [ ] `mvn -f backend/pom.xml test` passes.
- [ ] Repo docs and code are aligned on the v1 financial model.

## Suggested follow-up issues (next milestones)
- Connect this aligned model to Russian-specific tax/investment scenario rules in Milestone 11.
- Add explicit FX snapshot inputs if single-currency consolidated FIRE calculations become a v1 requirement.
- Add user-visible planning/scenario flows once the truth/planning split is stable in backend contracts.

## Task notes
- Treat `docs/domain/ТЗ-05...09` as the canonical product-domain source while this milestone is in progress.
- Prefer additive migrations and compatibility adapters over hard breaks where possible.
- If an implementation intentionally deviates from the docs, update the docs in the same PR rather than letting drift accumulate.
