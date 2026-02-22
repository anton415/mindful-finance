# Milestone 5 — Ingestion & Peace Calculator (v0)

## Goal / Done definition
- **Goal:** ingest transactions from a simple export format and compute first “Peace” signals from the Truth data.
- **Done when:**
  - `mvn -f backend/pom.xml test` passes from repo root.
  - An ingestion path exists (API endpoint + use case) that imports transactions idempotently.
  - First peace metrics exist as pure application use cases with tests (no UI needed yet).

## Summary (scope)
- **In scope:** one supported import format (start with CSV), dedupe/idempotency, import summary, and a small set of peace metrics.
- **Out of scope:** connecting to bank APIs, complex categorization, ML, FX conversion, forecasts.

## Import format (start simple)
CSV (suggested columns):
- `occurred_on` (YYYY-MM-DD)
- `direction` (INFLOW/OUTFLOW)
- `amount` (decimal, positive)
- `currency` (ISO-4217)
- `memo` (string, optional)

## Peace metrics (v0 candidates)
Pick 2–3 to start (recommended first):
- **Monthly burn (by currency):** total OUTFLOW over last 30/31 days.
- **Monthly savings (by currency):** INFLOW − OUTFLOW over last 30/31 days.
- **Runway (months):** liquid balance / monthly burn (only if burn > 0).

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Add ingestion use case**
- Add an application use case like `ImportTransactionsFromCsv`.
- Keep parsing at the edge (adapter) and pass validated inputs to the use case.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Add idempotency**
- Define a stable “import key” strategy (e.g., hash of occurredOn + direction + amount + currency + memo).
- Enforce uniqueness at the persistence layer (unique constraint) or application layer (existing lookup).
- Checkpoint: import same file twice ⇒ second run imports 0 new rows.

3) **Add peace metrics use cases**
- Implement 2–3 metrics as application use cases, using existing repositories.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Expose via API**
- Add endpoints like `POST /imports/transactions/csv` and `GET /peace/*`.
- Checkpoint: manual smoke test with a small CSV.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] CSV import works end-to-end via API.
- [ ] Import is idempotent (repeat import does not duplicate).
- [ ] 2–3 peace metrics exist as application use cases with unit tests.
- [ ] `mvn -f backend/pom.xml test` passes.

## Assumptions / defaults (explicit)
- Import is per-account (you pass `accountId` along with the file).
- Truth > convenience: invalid rows fail fast with a clear error message.

## Suggested follow-up issues (next milestones)
- Add richer ingestion formats (OFX/QFX) and mapping rules.
- Add UI visualization (Phase 3).
