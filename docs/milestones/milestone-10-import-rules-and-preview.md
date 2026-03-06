# Milestone 10 — Import Rules & Preview (v1)

## Goal / Done definition
- **Goal:** make transaction import safer and less repetitive by adding richer import formats, preview, and normalization rules.
- **Done when:**
  - The system supports at least one richer import format beyond CSV.
  - User can preview an import before saving it.
  - User can define simple normalization rules for noisy payee/memo data.
  - Import results remain idempotent and user-readable.

## Summary (scope)
- **In scope:** additional import format support (for example OFX/QFX), preview flow, normalization rules, UI for rule management, duplicate review summary.
- **Out of scope:** live bank connections, ML categorization, full personal-finance categorization system, OCR from PDFs.

## Facts
- Milestone 5 introduced CSV import and idempotent ingestion basics.
- Current import flow is useful, but still manual and format-limited.

## Assumptions / defaults (explicit)
- v1 adds one new import format only, rather than solving every bank export at once.
- Normalization rules are deterministic string rules, not ML-based matching.
- Preview happens before persistence and shows imported/skipped counts and representative rows.

## Open questions
- Which format should land first in v1: OFX or QFX?
- Should normalization rules apply only during import, or also retroactively later?

## Step-by-step implementation plan (with checkpoints)
1) **Add new import parser**
- Introduce one richer import format and normalize it to application inputs.
- Checkpoint: `mvn -f backend/pom.xml test`

2) **Add preview flow**
- Allow user to inspect what will be imported before final confirmation.
- Checkpoint: `mvn -f backend/pom.xml test`

3) **Add normalization rules**
- Support simple payee/memo transformation rules during import.
- Checkpoint: `mvn -f backend/pom.xml test`

4) **Expose UI for preview + rules**
- Add frontend flows for upload, preview, confirm, and saved rule management.
- Checkpoint: `npm -C frontend run build`

5) **Document supported behavior**
- Describe format limitations, preview semantics, and idempotency guarantees.
- Checkpoint: `mvn -f backend/pom.xml test && npm -C frontend run build`

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] System supports at least one richer import format beyond CSV.
- [ ] User can preview import results before saving.
- [ ] User can define and reuse normalization rules for payee/memo cleanup.
- [ ] Import remains idempotent after preview + confirmation flow.
- [ ] Frontend build passes (`npm -C frontend run build`).
- [ ] Backend tests pass (`mvn -f backend/pom.xml test`).

## Suggested follow-up issues (next milestones)
- Add mapping rules per bank/export source.
- Add category tagging during import.
- Add import source health diagnostics.

## Task notes
- Keep parsing at the edge and pass validated inputs into the application layer.
- Do not let preview logic bypass idempotency checks.
