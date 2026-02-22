# Milestone 2 — Application Kernel (Use Cases v0)

## Goal / Done definition
- **Goal:** add a pure Java 21 **application layer** (no Spring, no DB) that orchestrates the domain via ports + use cases and ships the first “Truth” calculations (balances / net worth).
- **Done when:**
  - `mvn -f backend/pom.xml test` passes from repo root.
  - `backend/application` module exists and depends only on `backend/domain` (no Spring/JPA).
  - First use cases exist with tests: compute account balance + net worth (by currency).
  - Repository ports exist for accounts + transactions (in-memory implementations only for tests are fine).

## Summary (scope)
- **In scope:** Maven module `application`, port interfaces, use-case classes, in-memory test fakes, and unit tests.
- **Out of scope:** Spring Boot app module, REST APIs, PostgreSQL schema/migrations, ingestion/parsing, currency conversion, UI.

## Locked decisions (so far)
- Maven aggregator lives at `backend/pom.xml`.
- Base package + groupId: `com.mindfulfinance`.
- Domain stays framework-free; money logic stays `BigDecimal` + strict scale (`Money`).

## Repo / module layout
Add:
- `backend/application/pom.xml` (packaging `jar`, depends on `domain`)
- `backend/application/src/main/java/com/mindfulfinance/application/**`
- `backend/application/src/test/java/com/mindfulfinance/application/**`

Update:
- `backend/pom.xml` (add module `application`)

## Public application APIs to implement (ports + use cases)

### Ports (interfaces)
Target API (signature-level):
```java
interface AccountRepository {
  Optional<Account> find(AccountId id);
  void save(Account account);
  List<Account> findAll();
}

interface TransactionRepository {
  List<Transaction> findByAccountId(AccountId accountId);
  void save(Transaction transaction);
}
```

Notes:
- Keep these as **ports** (interfaces) only. Real persistence adapters come later.
- It’s OK if in-memory implementations live under `src/test` for now.

### Use cases (thin vertical slices)

#### `ComputeAccountBalance`
Input:
- `AccountId`

Behavior:
- Load the account and its transactions.
- Reject currency mismatches (`transaction.amount().currency()` must equal `account.currency()`).
- Sum `Transaction.signedAmount()` starting from `Money.zero(account.currency())`.

Output:
- `Money` in the account’s currency.

#### `ComputeNetWorthByCurrency`
Behavior:
- For all **active** accounts, compute balances and aggregate by currency.

Output:
- `Map<Currency, Money>` (one bucket per currency).

Notes:
- No FX conversion yet (net worth stays currency-bucketed for truthfulness).

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Add Maven module**
- Add `backend/application` module and wire it into `backend/pom.xml`.
- Add JUnit 5 + Surefire (matching the domain module).
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Define ports**
- Add `AccountRepository` + `TransactionRepository`.
- Add minimal in-memory implementations (in `src/test`) to support use-case tests.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Implement `ComputeAccountBalance` + tests**
Test scenarios:
- No transactions ⇒ zero money in account currency.
- Inflows/outflows sum correctly.
- Any currency mismatch throws.
- Missing account throws (application-level error).
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Implement `ComputeNetWorthByCurrency` + tests**
Test scenarios:
- Multiple accounts, same currency ⇒ aggregated into one bucket.
- Multiple currencies ⇒ separate buckets.
- Archived accounts ignored.
- Checkpoint: `mvn -f backend/pom.xml test`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] `backend/application` builds with Java 21 (`mvn -f backend/pom.xml test`).
- [ ] `backend/application` has **no** Spring dependencies.
- [ ] Ports exist: `AccountRepository`, `TransactionRepository`.
- [ ] `ComputeAccountBalance` use case exists under `com.mindfulfinance.application.*` with unit tests.
- [ ] `ComputeNetWorthByCurrency` use case exists under `com.mindfulfinance.application.*` with unit tests.
- [ ] Net worth is grouped by currency (no FX conversion yet).

## Assumptions / defaults (explicit)
- “Account not found” is modeled as an **application error** (not a domain invariant).
- Currency mismatches are rejected at the application boundary until a richer multi-currency story exists.
- In-memory repositories are acceptable for tests; persistence choices are deferred.

## Suggested follow-up issues (next milestones)
- Add adapters module (e.g., Spring Boot) to expose REST endpoints.
- Add persistence adapter (PostgreSQL) + migrations.
- Add ingestion pipeline for bank exports (Roadmap Phase 2).
