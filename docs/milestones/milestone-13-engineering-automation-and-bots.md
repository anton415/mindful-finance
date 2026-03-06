# Milestone 13 — Инженерная автоматизация и боты (v1)

## Goal / Done definition
- **Goal:** make repository operations safer and more repeatable so product work can move with less manual regression checking and dependency drift.
- **Done when:**
  - Every PR runs backend tests plus frontend lint/build in GitHub Actions.
  - Dependency updates for Maven, npm, and GitHub Actions arrive automatically through a bot with a controlled cadence.
  - Dependency review and CodeQL scanning run in GitHub and surface supply-chain or code-scanning regressions early.
  - Architecture guard checks protect the framework-free domain/application boundary.
  - The repo has a documented path for manual deploy/smoke automation that can be connected to the closed beta later.

## Summary (scope)
- **In scope:** CI baseline, dependency/security bots, architecture guard automation, and manual automation hooks for beta operations.
- **Out of scope:** full CD to production on every merge, preview environments for each PR, advanced observability automation, and self-hosted runners.

## Facts
- The repository currently has a Java 21 Maven backend and a React/Vite frontend.
- Local checkpoints already rely on `mvn -f backend/pom.xml test`, `npm -C frontend run lint`, and `npm -C frontend run build`.
- PostgreSQL integration coverage already uses Testcontainers, so CI can exercise database-backed tests on GitHub-hosted runners.
- Milestone 12 expects a repeatable deployment path and smoke checks, but does not require full production CD yet.

## Assumptions / defaults (explicit)
- Dependabot is sufficient for the first automation pass; Renovate is not required unless update volume becomes hard to manage.
- CI should stay thin and deterministic: test, lint, build, and boundary checks first; deploy automation later.
- Security automation should fail PRs only on meaningful signals, not on every informational warning.
- Manual `workflow_dispatch` is the right first step for beta deploy/smoke automation before any auto-deploy policy exists.

## Open questions
- Should major dependency updates be grouped less aggressively than patch/minor updates?
- Do we want branch protection and required checks enabled immediately after the CI baseline lands?
- Should post-deploy smoke checks stop at `/api/health`, or also include login and one CRUD path once auth exists?

## Step-by-step implementation plan (with checkpoints)
1) **Add CI baseline**
- Create GitHub Actions jobs for backend tests and frontend lint/build on `pull_request` and `push` to `main`.
- Checkpoint: open a test PR and verify all required jobs start and pass.

2) **Add dependency/security bots**
- Configure Dependabot for Maven, npm, and GitHub Actions updates with weekly cadence and grouped patch/minor updates.
- Add Dependency Review and CodeQL workflows.
- Checkpoint: Dependabot preview/config validates in GitHub and security workflows appear in the Actions tab.

3) **Add architecture guard**
- Fail fast when Spring or persistence framework dependencies leak into `backend/domain` or `backend/application`.
- Checkpoint: intentional guard violation in a draft branch triggers a workflow failure.

4) **Wire repo governance**
- Mark CI and architecture guard jobs as required checks in branch protection.
- Define how dependency bot PRs should be reviewed and merged.
- Checkpoint: direct merge is blocked when required checks fail.

5) **Prepare beta automation hooks**
- Add manual deploy and post-deploy smoke workflows once the Yandex Cloud baseline is ready.
- Keep them explicit and operator-driven at first.
- Checkpoint: manual dry-run succeeds against the beta environment without hidden local steps.

## Acceptance criteria (copyable to a GitHub Issue)
- [ ] PRs trigger backend tests in GitHub Actions.
- [ ] PRs trigger frontend lint and production build in GitHub Actions.
- [ ] Dependabot opens update PRs for Maven, npm, and GitHub Actions on a defined cadence.
- [ ] Dependency Review runs on PRs and blocks on configured high-severity findings.
- [ ] CodeQL scans the Java and TypeScript codebase in GitHub.
- [ ] Architecture guard protects `backend/domain` and `backend/application` from framework leakage.
- [ ] Manual deploy/smoke automation tasks are tracked for the beta path.

## Suggested follow-up issues (next milestones)
- Add auto-triage or auto-merge policy for low-risk bot PRs once branch protection is stable.
- Add nightly regression jobs if FIRE calculations or import flows become too slow for per-PR checks.
- Add staging deploy automation after the first closed beta path is operational.

## Task notes
- Treat this as an engineering enablement milestone, not as a reason to bypass product-roadmap checkpoints.
- Keep domain truth explicit: automation should reinforce the architecture, not hide violations.
- Prefer low-maintenance GitHub-native automation before introducing extra bot platforms.
