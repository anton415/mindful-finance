# ТЗ-04. Ограничения и исключения v1

**Статус:** рабочий проект  
**Назначение документа:** зафиксировать перечень продуктовых, UX и platform-ограничений, сознательно исключаемых из первой версии Mindful Finance.  
**Связь с Техническим заданием:** материал используется при подготовке разделов об ограничениях проекта, составе первой версии и критериях отсечения feature creep.  
**Источник в репозитории:** `docs/product/04-v1-exclusions.md`

## 1. Назначение документа

Документ служит anti-scope guardrail для версии `v1 / closed beta` и помогает отличать обязательные требования первой версии от инициатив, которые должны быть перенесены в post-v1 backlog. Перечень исключений не означает отказ от соответствующих возможностей навсегда; он фиксирует только границу текущего релиза.

## 2. Принципы формирования исключений

В качестве исключений выбираются инициативы, которые:

- размывают calm-first product thesis;
- резко расширяют integration, support или platform surface;
- не являются необходимыми для подтверждения ценности `v1`;
- переводят продукт из focused closed beta в generic finance platform.

## 3. Перечень исключений версии v1

### 3.1. Публичная регистрация и growth funnel

В `v1` не входят open public signup, массовый acquisition funnel и growth loops. Первая версия должна оставаться invited closed beta с контролируемым составом пользователей.

### 3.2. Attention-driven finance UX

В `v1` не входят notification-heavy engagement loops, reactive day-trading dashboards и интерфейсы, оптимизированные под частоту открытия вместо качества review.

### 3.3. Full bank sync и live integrations

В `v1` не входят direct bank connections, live brokerage sync и always-on external ingestion. Для первой версии достаточно reliable import flows и truthful manual or preview-based ingestion.

### 3.4. Расширенный identity scope

В `v1` не входят OAuth/social auth и широкий набор identity-provider integrations. Для первой closed beta достаточно прагматичного email/password baseline при надежной user-level isolation.

### 3.5. Mobile delivery scope

В `v1` не входят отдельные native iOS и Android applications. На текущем этапе необходимо сначала подтвердить полезность core web workflow.

### 3.6. ML-first automation

В `v1` не входят ML categorization, непрозрачные эвристики и AI-heavy automation как основное обещание продукта. Первая версия должна опираться на deterministic truth и explainable rules.

### 3.7. Generic global finance coverage

В `v1` не входят multi-country tax engines и универсальная global coverage для всех юрисдикций. Дифференциация первой версии строится на calm-first planning и российском контексте, а не на попытке сразу охватить весь рынок.

### 3.8. Market prediction и speculative analytics

В `v1` не входят brokerage forecasting, market-price prediction и speculative analytics, выходящая за пределы explainable scenario planning.

### 3.9. Shared household collaboration

В `v1` не входят family planning, shared household goals и сложные multi-user collaboration flows beyond strict personal data isolation.

### 3.10. Избыточная platform complexity

В `v1` не входят Kubernetes, advanced observability stack, staging matrix и engineering automation как релизный блокер. Для первой версии важнее repeatable deploy path, backups и smoke checks.

## 4. Что должно оптимизироваться вместо исключенных направлений

Версия `v1` должна оптимизироваться для trustworthy financial truth, спокойного weekly или monthly review, explainable planning signals, безопасного import/edit workflow и pragmatic closed beta delivery.
