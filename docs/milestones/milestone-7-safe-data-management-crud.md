# Milestone 7 — Safe Data Management (CRUD v1)

## Goal / Done definition
- **Goal:** let a user safely correct or remove existing accounts and transactions without direct DB edits.
- **Done when:**
  - Transactions can be edited and deleted from API + UI.
  - Accounts can be edited and deleted from API + UI.
  - Deletion rules are explicit and enforced by the domain/application layers.
  - Backend tests and frontend build pass after the CRUD additions.

## Summary (scope)
- **In scope:** account/transaction update + delete flows, domain invariants, API contracts, PostgreSQL persistence, Calm UI confirmations and refresh behavior.
- **Out of scope:** audit log, undo history, soft delete infrastructure, multi-user permissions, bulk edit tools.

## Facts
- The current MVP already supports listing accounts, viewing transactions, creating accounts, creating transactions, and importing CSV transactions.
- Current API surface includes `GET` + `POST` flows, but no account/transaction update or delete endpoints.
- Milestone 6 shipped a Russian-localized UI with account detail interactions.

## Assumptions / defaults (explicit)
- Transaction edits can change date, direction, amount, and memo, but stay inside the same account in v1.
- Account edits can change name and type; currency remains immutable after creation in v1.
- Account deletion is allowed only when the account has no remaining transactions.
- Hard delete is acceptable for v1, as long as the rule is explicit and tested.

## Open questions
- Should account deletion be blocked with a domain error or require a force-delete flow after transaction cleanup?
- Should transaction editing preserve original create timestamps or add explicit updated timestamps later?
- Do we need optimistic UI updates in React, or is explicit reload-after-success sufficient for this slice?

## Step-by-step implementation plan (with checkpoints)
1) **Define CRUD contracts + invariants**
- Confirm update/delete rules for accounts and transactions across domain, application, and HTTP layers.
- Write or update tests that describe the intended behavior before implementation.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Edit transaction slice**
- Add application/API/persistence support for updating transaction fields.
- Ensure balance and downstream Peace calculations reflect the edited transaction values.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Delete transaction slice**
- Add transaction deletion path and verify account balances are recalculated correctly after removal.
- Add UI affordance with calm confirmation.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

4) **Edit account slice**
- Add account metadata editing for supported fields.
- Reflect changes in account list/detail views without noisy UX.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

5) **Delete account slice**
- Enforce the chosen deletion rule in application/domain logic and surface a clear API/UI message when blocked.
- Allow deletion only when invariants are satisfied.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

6) **Polish + documentation**
- Update README/frontend docs/API notes to describe the new CRUD behavior and any deletion restrictions.
- Add manual QA notes for the four key flows.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] User can edit an existing transaction from the UI.
- [ ] User can delete an existing transaction from the UI.
- [ ] User can edit an existing account from the UI.
- [ ] User can delete an existing account from the UI when deletion rules allow it.
- [ ] API exposes tested update/delete endpoints for accounts and transactions.
- [ ] PostgreSQL adapters persist CRUD changes correctly.
- [ ] Failed delete cases return clear, user-safe errors.
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).
- [ ] Frontend build passes (`npm -C frontend run build`).

## Suggested follow-up issues (next milestones)
- Add audit trail and activity history for data changes.
- Add undo/restore support for destructive actions.
- Add recurring transaction management and template editing.

## Task notes
- Prefer thin vertical slices: one user-visible behavior at a time.
- Keep money logic inside the domain/application boundary and continue to use `BigDecimal`-based invariants.
- Avoid introducing framework-specific logic into domain modules while adding CRUD support.
