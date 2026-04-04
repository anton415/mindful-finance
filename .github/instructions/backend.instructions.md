---
applyTo: "backend/**/*.java,backend/**/*.xml,backend/pom.xml"
description: Mindful Finance backend rules
---

- Prefer inside-out slices: domain -> application -> adapters -> api.
- State which module is the source of truth before editing.
- Keep each slice narrow: one invariant, one use case seam, one mapping, or one endpoint behavior.
- Keep `backend/domain` and `backend/application` framework-free.
- Do not introduce Spring/JPA/JDBC/servlet imports or annotations into domain or application.
- Keep controllers thin.
- Money is exact only: use `BigDecimal` or `Money`; make scale, currency, and rounding explicit.
- Add or update tests close to the changed behavior.
- Review must explicitly check naming, boundaries, testability, file size, and hidden state coupling.