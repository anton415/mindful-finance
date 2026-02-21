# Milestone 1 — Domain Kernel (Truth Engine v0)

## Goal / Done definition
- **Goal:** establish a pure Java 21 Core Domain (no Spring, no DB) that encodes “Truth” via strict invariants and immutable records.
- **Done when:**
  - `mvn -f backend/pom.xml test` passes from repo root.
  - Domain module has **no** Spring/JPA dependencies.
  - Money is `BigDecimal`-based (no floating point), and core records exist with invariant tests.

## Summary (scope)
- **In scope:** Maven backend scaffold + `domain` module, initial domain types (`Money`, `Account`, `Transaction`, `LifeGoal`), invariants, minimal unit tests, and a short README update for running tests.
- **Out of scope:** Spring Boot app module, REST APIs, PostgreSQL schema/migrations, ingestion, currency conversion, UI, “Peace Evaluator” calculations.

## Locked decisions (from you)
- Maven aggregator lives at `backend/pom.xml`.
- Base package + groupId: `com.mindfulfinance`.
- Transaction model: `direction + positive Money` (sign derived via helper method).

## Repo / module layout
Create:
- `backend/pom.xml` (packaging `pom`, modules include `domain`)
- `backend/domain/pom.xml` (packaging `jar`)
- `backend/domain/src/main/java/com/mindfulfinance/domain/**`
- `backend/domain/src/test/java/com/mindfulfinance/domain/**`

## Public domain APIs to implement (types + invariants)

### `Money`
Target API (signature-level):
```java
record Money(BigDecimal amount, Currency currency) { ... }
```
Invariants:
- `amount != null`, `currency != null`
- **Strict scale truth:** normalize to `currency.getDefaultFractionDigits()` by padding zeros, but **reject** inputs with more fractional digits (no rounding).
Operations:
- `Money zero(Currency c)`
- `add/subtract` require same currency
- `negated`, `signum`, `isZero/isPositive/isNegative` (optional convenience)

### IDs (value objects)
- `record AccountId(UUID value)`, `record TransactionId(UUID value)`, `record LifeGoalId(UUID value)`
- Invariants: `value != null`
- Convenience: `static random()` (mainly for tests)

### `Account`
```java
record Account(
  AccountId id,
  String name,
  Currency currency,
  AccountType type,
  AccountStatus status,
  Instant createdAt
) { ... }
```
Invariants:
- Non-null fields; `name` trimmed and non-blank.
- `currency` is the account’s currency (matching transactions enforced later in use cases).
Behavior helpers (small, pure):
- `boolean isActive()`
- `Account archive()` (idempotent)

Enums:
- `AccountType` (start minimal: CASH, DEPOSIT, FUND, IIS, BROKERAGE)
- `AccountStatus` (ACTIVE, ARCHIVED)

### `Transaction`
```java
record Transaction(
  TransactionId id,
  AccountId accountId,
  LocalDate occurredOn,
  TransactionDirection direction,
  Money amount,
  String memo,
  Instant createdAt
) { ... }
```
Invariants:
- All non-null except `memo` (optional).
- `amount > 0` always (strict).
- `memo`: trim; treat blank as `null`.
Behavior:
- `Money signedAmount()` returns negative for OUTFLOW, positive for INFLOW.

Enum:
- `TransactionDirection` (INFLOW, OUTFLOW)

### `LifeGoal`
```java
record LifeGoal(
  LifeGoalId id,
  String title,
  Money targetAmount,
  LocalDate targetDate,
  LifeGoalStatus status,
  String notes,
  Instant createdAt
) { ... }
```
Invariants:
- Non-null required fields.
- `title` trimmed and non-blank.
- `targetAmount > 0` always.
- `notes` optional; trim and null blank.
Behavior helper:
- `boolean isActive()`

Enum:
- `LifeGoalStatus` (ACTIVE, ACHIEVED, ARCHIVED)

### Shared helper (small)
- `Preconditions.requireNonBlank(String value, String fieldName)` returning trimmed string or throwing.

## Step-by-step implementation plan (with checkpoints)
You write the code; each step ends with a **check** you can run.

1) **Prep sanity**
- Checkpoint: `git status` is clean.
- If you have leftover local build artifacts under `backend/**/target`, delete them to avoid confusion (optional housekeeping).

2) **Maven backend scaffold**
- Create `backend/pom.xml` (aggregator, `packaging=pom`, module `domain`).
- Create `backend/domain/pom.xml` (jar).
- Configure Java compilation as **release 21** and JUnit 5 + Surefire.
- Checkpoint: `mvn -f backend/pom.xml test` succeeds (even with 0 tests).

3) **Domain package skeleton**
- Create package folders under `backend/domain/src/main/java/com/mindfulfinance/domain/`.
- Add a tiny placeholder type to ensure compilation wiring is correct.
- Checkpoint: `mvn -f backend/pom.xml test` still succeeds.

4) **Implement `Money` first**
- Implement `Money` invariants and operations.
- Add `MoneyTest`.
Test scenarios:
  - Pads scale: `new Money(1, USD)` becomes `1.00` (USD).
  - Rejects too many decimals: `1.001 USD` throws.
  - `add` with different currency throws.
- Checkpoint: `mvn -f backend/pom.xml test`.

5) **Implement `Account` + tests**
- Add `AccountId`, `AccountType`, `AccountStatus`, `Account`.
Test scenarios:
  - Name trims (`"  Main  "` => `"Main"`).
  - Blank name throws.
  - `archive()` is idempotent and preserves other fields.
- Checkpoint: `mvn -f backend/pom.xml test`.

6) **Implement `Transaction` + tests**
- Add `TransactionId`, `TransactionDirection`, `Transaction`.
Test scenarios:
  - Amount must be > 0 (0 and negative throw).
  - `memo` trims; blank becomes `null`.
  - `signedAmount()` is negative for OUTFLOW.
- Checkpoint: `mvn -f backend/pom.xml test`.

7) **Implement `LifeGoal` + tests**
- Add `LifeGoalId`, `LifeGoalStatus`, `LifeGoal`.
Test scenarios:
  - `targetAmount > 0` enforced.
  - Title trims; blank throws.
- Checkpoint: `mvn -f backend/pom.xml test`.

8) **Repo hygiene + docs**
- Update `.gitignore` to ignore Maven outputs (`**/target/`) (and any IDE entries you want).
- Update `README.md` with: `mvn -f backend/pom.xml test`.
- Final checkpoint: fresh `mvn -f backend/pom.xml test` passes.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] `backend/domain` builds with Java 21 (`mvn -f backend/pom.xml test`).
- [ ] Domain module has **no** Spring dependencies.
- [ ] `Money` uses `BigDecimal` + strict scale rules (no rounding).
- [ ] `Account`, `Transaction`, `LifeGoal` records exist under `com.mindfulfinance.domain.*` with invariants.
- [ ] Unit tests cover key invariants (scale, non-blank names/titles, positive amounts, sign model).
- [ ] README contains the test command.

## Assumptions / defaults (explicit)
- Currency is stored on `Money` and on `Account`; “transaction currency must match account currency” will be enforced later in application/use-case layer.
- `createdAt` is `Instant`; human-relevant dates use `LocalDate`.
- IDs are UUID-based value objects for now (DB concerns deferred).

## Suggested follow-up issues (next milestones)
- Add application module (use cases) + ports (repositories) without picking a persistence tech yet.
- Add first calculation use-case: “Account balance” / “Net worth” computed from transactions (still no DB).
- Add Spring Boot adapter module + PostgreSQL persistence once domain + use cases stabilize.