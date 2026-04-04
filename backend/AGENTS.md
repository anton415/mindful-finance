This file adds backend-local rules on top of the root [`AGENTS.md`](../AGENTS.md).

Scope
- Applies to `backend/**`.
- Keep the root mini brief, facts/assumptions/open questions, thin-slice planning, checkpoint-first workflow, and retrospective.

Backend operating model
- Prefer inside-out slices: domain -> application -> adapters (`postgres`) -> transport (`api`).
- Before editing, state which module is the source of truth for the slice.
- Keep each slice narrow: one invariant, one use case seam, one repository mapping, or one endpoint behavior at a time.
- Pick one checkpoint command and expected result before editing:
  - Domain or application slice: `mvn --batch-mode -f backend/pom.xml -pl domain,application test`
  - Adapter or API slice: `mvn --batch-mode -f backend/pom.xml test`
- Final backend checkpoint: `mvn --batch-mode -f backend/pom.xml test`

Architecture boundaries
- `backend/domain` and `backend/application` must stay framework-free.
- Do not add Spring, servlet, JDBC, JPA, or persistence annotations and imports to `domain` or `application`.
- Keep business rules and money logic out of controllers and infrastructure adapters.
- Adapters translate between framework or storage details and application ports. Do not leak transport DTOs, SQL types, or framework objects into domain logic.

Controllers and transport
- Controllers must be thin: parse transport input, delegate to explicit use cases, map transport output, and handle HTTP concerns.
- Do not hide business decisions in controllers, request mappers, or response assemblers.
- If a controller is already large, prefer extracting one endpoint flow or one mapper seam per slice instead of expanding the controller further.

Use case design
- Keep use cases small, explicit, and named by intent.
- One use case should make one main decision flow obvious.
- If branching or policy logic grows, extract a domain policy, helper, or value object with explicit inputs and outputs instead of growing orchestration code.
- Prefer constructor-injected dependencies and explicit return values over hidden mutable state.

Money rules
- Money is `BigDecimal` only. Never introduce `float` or `double` for monetary logic.
- Prefer the domain `Money` value object at domain and application boundaries.
- Raw `BigDecimal` is acceptable only at clear storage or transport edges, or inside `Money` itself.
- Make scale, currency, and rounding explicit. Never rely on implicit floating conversion or silent rounding.
- Tests for money logic must assert exact values, currency, and rounding behavior.

Testing and review
- Add or update tests as close as possible to the changed behavior:
  - Domain tests for invariants and value objects.
  - Application tests for orchestration and port interactions.
  - Postgres tests for mapping and persistence behavior.
  - API tests for request and response contracts.
- In review, explicitly check naming, boundaries, testability, file size, and hidden state coupling.
- Use [`../code_review.md`](../code_review.md) as the review checklist.
