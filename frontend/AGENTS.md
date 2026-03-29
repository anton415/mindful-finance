This file adds frontend-local rules on top of the root [`AGENTS.md`](../AGENTS.md).

Scope
- Applies to `frontend/**`.
- Keep the root mini brief, facts/assumptions/open questions, thin-slice planning, checkpoint-first workflow, and retrospective.

Frontend operating model
- Work in thin vertical slices with visible behavior checkpoints. No big-bang rewrite.
- Before editing, name the seam you are changing: pure helper, custom hook, presentational component, API boundary, or page-level orchestration.
- Keep each slice focused on one behavior or one extraction seam at a time.
- Pick one checkpoint command and expected result before editing:
  - Fast local checkpoint: `npm --prefix frontend run lint`
  - Final frontend checkpoint: `npm --prefix frontend run build`
  - If tests exist or you add them, run the narrowest relevant test command too and state why it covers the slice.

Refactor rules
- Do not do a broad rewrite of a feature or page to "clean it up".
- Prefer extracting one pure helper, one custom hook, or one presentational component at a time.
- Before a large or risky refactor, add a safety-net test first. If no test harness exists yet, say so and make the smallest reasonable plan to introduce one before deeper surgery.
- Avoid changing state shape, markup, API wiring, and styling in the same slice unless the task explicitly requires it.

Structure guidance
- Put pure calculations, formatting, and mapping into pure helpers.
- Put effectful orchestration and state transitions into custom hooks with explicit inputs and outputs.
- Put render-heavy branches into presentational components driven by props.
- Keep transport and API concerns near feature boundaries, not hidden deep in presentational code.
- Before extraction, make hidden state coupling explicit: identify the props, local state, effects, refs, async status, and derived values that the extraction depends on.

Large-file guidance
- If you touch a large hotspot such as `src/features/personal-finance/PersonalFinanceView.tsx`, default to one seam per slice.
- Prefer shrinking the hotspot incrementally instead of moving many concerns at once.
- Make behavior preservation explicit with a checkpoint after each extraction.

Testing and review
- Add tests before major refactors. For smaller extractions, preserve behavior and verify with lint, build, and a focused manual scenario.
- In review, explicitly check naming, boundaries, testability, file size, and hidden state coupling.
- Use [`../code_review.md`](../code_review.md) as the review checklist.
