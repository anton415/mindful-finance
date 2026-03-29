# Code Review Guide

Use this document for review requests and self-review before finishing a slice.

## Review operating model

- Start by clarifying scope if needed with a mini brief:
  - Goal
  - Done
  - Constraints
  - Non-goals
- Separate facts, assumptions, and open questions before judging the change.
- Findings come first. Summaries are secondary.
- Each finding should explain the problem, why it matters, and the likely impact on behavior, maintenance, or future changes.
- Verify concerns with the smallest relevant command, test, or scenario when possible.

## Core review checklist

### Naming

- Do names reflect domain intent and user-facing behavior rather than implementation accidents?
- Are classes, functions, hooks, and variables specific enough to be searchable and understandable?
- Are overloaded or ambiguous names hiding multiple responsibilities?

### Boundaries

- Does the change preserve the intended boundaries between domain, application, adapters, and transport?
- Are framework details kept out of backend domain and application code?
- Are frontend presentational concerns kept separate from API orchestration and stateful effects?
- Are controllers, hooks, helpers, and repositories doing only the work that belongs to them?

### Testability

- Can the important logic be exercised without hidden framework setup, global state, or timing tricks?
- Are the key decisions isolated enough to test at the right level?
- Does the change make future tests easier rather than harder?

### File size and cohesion

- Is the file, class, or component growing into a hotspot with too many responsibilities?
- If the file is already large, did the change reduce or increase the concentration of logic?
- Could one seam be extracted now without turning the change into a rewrite?

### Hidden state coupling

- Is behavior spread across implicit state, effects, mutable fields, or ordering assumptions?
- Are there dependencies between props, local state, repository calls, async statuses, or side effects that are not explicit in the API?
- Does the change introduce state that can drift out of sync with the real source of truth?

## Backend-specific checks

- Money logic uses `BigDecimal` or the domain `Money` model only.
- Domain and application stay framework-free.
- Controllers stay thin.
- Use cases are small, explicit, and named by intent.
- Adapters translate infrastructure concerns without leaking them upward.

## Frontend-specific checks

- The change is incremental, not a big-bang rewrite.
- Pure helpers, custom hooks, and presentational components are extracted only when their inputs and outputs are explicit.
- Large refactors have a safety-net test first, or the absence of such a test is called out as risk.
- Effects do not coordinate unrelated concerns without a clear reason.

## Review output model

- List findings first, ordered by severity.
- Reference the exact file and line when possible.
- After findings, note open questions, assumptions, or residual risks.
- If there are no findings, say that explicitly and still mention any testing gaps or review limits.
