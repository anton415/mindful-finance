---
applyTo: "frontend/**/*.{ts,tsx,css},frontend/package.json,frontend/vite.config.ts"
description: Mindful Finance frontend rules
---

- No big-bang rewrites.
- Before editing, name the seam: pure helper, custom hook, presentational component, API boundary, or page orchestration.
- Change one behavior or one extraction seam per slice.
- Before risky refactors, prefer a safety-net test. If no test harness exists, call out the risk and propose the smallest path to introduce one.
- Avoid changing state shape, markup, API wiring, and styling in the same slice unless explicitly required.
- Use `npm --prefix frontend run lint` as the fast checkpoint and `npm --prefix frontend run build` as the final checkpoint.
- Review must explicitly check naming, boundaries, testability, file size, and hidden state coupling.