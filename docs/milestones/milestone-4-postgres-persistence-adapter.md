# Milestone 4 — PostgreSQL Persistence Adapter (v0)

## Goal / Done definition
- **Goal:** persist accounts + transactions in PostgreSQL via an adapter that implements application ports, keeping `domain` + `application` clean.
- **Done when:**
  - `mvn -f backend/pom.xml test` passes from repo root.
  - A Postgres-backed adapter exists (suggested: `backend/postgres`) implementing the `application` repository ports.
  - Migrations exist (suggested: Flyway) to create the schema.
  - API module can run end-to-end using Postgres locally.

## Summary (scope)
- **In scope:** persistence module wiring, schema migrations, repository implementations, integration tests (prefer Testcontainers).
- **Out of scope:** performance tuning, multi-tenant, encryption at rest, FX conversion, ingestion logic.

## Open choices (pick one; recommended first)
- **Migration tool:** Flyway.
- **DB access:** Spring JDBC / `JdbcTemplate`.

## Repo / module layout
Add:
- `backend/postgres/pom.xml`
- `backend/postgres/src/main/java/com/mindfulfinance/postgres/**`
- `backend/postgres/src/test/java/com/mindfulfinance/postgres/**`
- `backend/postgres/src/main/resources/db/migration/**`
- `backend/docker-compose.yml` (or repo-root `docker-compose.yml`)

Update:
- `backend/pom.xml` (add module `postgres`)
- `backend/api` module wiring to use the Postgres adapter

## Data model (v0)
Suggested tables:
- `accounts` (id, name, currency, type, status, created_at)
- `transactions` (id, account_id, occurred_on, direction, amount, currency, memo, created_at)

Notes:
- Store `Money` as `(amount NUMERIC, currency CHAR(3))`.
- Keep `Transaction.amount` **positive** and store `direction` separately (matches domain model).

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Add Postgres module**
- Create `backend/postgres` and wire into `backend/pom.xml`.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Add migrations**
- Add initial schema migration(s).
- Checkpoint: run local Postgres + verify tables exist (via psql or Testcontainers test).

3) **Implement repositories**
- Implement `AccountRepository` + `TransactionRepository` ports using JDBC.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Integration tests**
- Use Testcontainers to validate: save/find account, save/list transactions, findByAccountId ordering.
- Checkpoint: `mvn -f backend/pom.xml test`

5) **Wire API to Postgres**
- Configure `backend/api` to use the Postgres adapter (Spring profiles recommended: `inmemory` vs `postgres`).
- Checkpoint: `mvn -f backend/api/pom.xml spring-boot:run` + smoke endpoints.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] Postgres schema migrations exist and create `accounts` + `transactions`.
- [ ] Persistence adapter implements `AccountRepository` + `TransactionRepository`.
- [ ] Integration tests run against Postgres (prefer Testcontainers).
- [ ] API runs end-to-end with Postgres locally.
- [ ] `mvn -f backend/pom.xml test` passes.

## Assumptions / defaults (explicit)
- Local dev DB uses Docker Compose.
- No FX conversion; currency codes stored as ISO-4217 (3 letters).

## Suggested follow-up issues (next milestones)
- Add ingestion/import pipeline (Phase 2).
- Add read-model optimizations (indexes, queries) once usage patterns exist.
