# Milestone 3 — HTTP API Adapter (Spring Boot v0)

## Goal / Done definition
- **Goal:** expose the application use cases over HTTP via a Spring Boot adapter, while keeping `domain` + `application` framework-free.
- **Done when:**
  - `mvn -f backend/pom.xml test` passes from repo root.
  - A new Spring Boot module exists (suggested: `backend/api`) that depends on `backend/application`.
  - Minimal REST endpoints exist for accounts + transactions + read models (balance / net worth).
  - Adapter layer maps domain/application errors to clear HTTP responses (400/404/409).

## Summary (scope)
- **In scope:** Spring Boot module wiring, controllers, DTOs, error mapping, and adapter-level tests.
- **Out of scope:** authentication/authorization, UI, production persistence (Postgres comes next), FX conversion, ingestion.

## Open choices (pick one; recommended first)
- **Module name:** `backend/api` (recommended) vs `backend/adapters/http`.
- **Testing style:** controller tests via `@WebMvcTest` (fast) vs full `@SpringBootTest` (more wiring coverage).

## Repo / module layout
Add:
- `backend/api/pom.xml`
- `backend/api/src/main/java/com/mindfulfinance/api/**`
- `backend/api/src/test/java/com/mindfulfinance/api/**`

Update:
- `backend/pom.xml` (add module `api`)

## HTTP API surface (v0)

### Endpoints (suggested)
- `GET /health` → `{ "status": "ok" }`
- `POST /accounts` → creates an account
- `GET /accounts` → lists accounts
- `POST /accounts/{accountId}/transactions` → creates a transaction for an account
- `GET /accounts/{accountId}/transactions` → lists transactions
- `GET /accounts/{accountId}/balance` → returns `Money`
- `GET /net-worth` → returns net worth grouped by currency

### Error mapping (suggested)
- Validation / invariant failure → `400 Bad Request`
- Missing account → `404 Not Found`
- Idempotency/duplicate (if added) → `409 Conflict`

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Add Spring Boot module**
- Create `backend/api/pom.xml` (Spring Boot 3.x, Java 21).
- Wire `api` into `backend/pom.xml`.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Wire application layer**
- Add Spring beans for ports + use cases.
- For now, use **in-memory** repository adapters (can live under `src/main` or `src/test` while iterating).
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add controllers + DTOs**
- Implement the endpoints listed above.
- Keep DTOs in the `api` module; don’t leak Spring annotations into `application` or `domain`.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Add error handling**
- Add one `@ControllerAdvice` to map exceptions to HTTP responses consistently.
- Checkpoint: `mvn -f backend/pom.xml test`

5) **Manual smoke test**
- Run: `mvn -f backend/api/pom.xml spring-boot:run`
- Checkpoint: `curl -s localhost:8080/health` returns `{"status":"ok"}`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] `backend/api` module exists and builds with Java 21.
- [ ] `backend/api` depends on `backend/application` (and indirectly `domain`).
- [ ] Controllers exist for accounts + transactions + balance + net worth.
- [ ] Error mapping is consistent (400/404/409).
- [ ] `mvn -f backend/pom.xml test` passes.

## Assumptions / defaults (explicit)
- Persistence is **in-memory only** for v0 (Postgres persistence arrives in Milestone 4).
- API is unauthenticated for now (local-only developer MVP).

## Suggested follow-up issues (next milestones)
- Add Postgres persistence adapter + migrations.
- Add ingestion endpoint for bank exports (Phase 2).
