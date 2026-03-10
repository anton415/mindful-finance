# ТЗ-02. Границы версии v1

**Статус:** рабочий проект  
**Назначение документа:** зафиксировать продуктовые границы первой внешне доступной версии Mindful Finance и перечень условий, при которых версия считается готовой к использованию.  
**Связь с Техническим заданием:** материал используется при подготовке разделов о составе функций, ограничениях релиза, допущениях и критериях готовности первой версии.  
**Источник в репозитории:** `docs/product/02-v1-scope.md`

## 1. Назначение версии v1

Версия `v1` рассматривается как первая externally usable версия Mindful Finance, предназначенная для небольшой приглашенной группы пользователей. Цель выпуска состоит в проверке продуктовой тезы без размывания продукта в generic finance application.

## 2. Критерии готовности версии

Версия `v1` считается готовой при одновременном выполнении следующих условий:

- продукт доступен invited users как closed beta по публичному HTTPS-домену;
- пользователь проходит аутентификацию и видит только собственные данные;
- поддерживаются базовые end-to-end сценарии учета, импорта, исправления данных, recurring flows, goals и planning review;
- система показывает calm financial truth и explainable planning signals, а не только ledger maintenance;
- Russian FIRE calculations представлены как scenario modeling с явными assumptions и ограничениями;
- процедуры deploy, backup, restore и smoke checks документированы и воспроизводимы.

## 3. Фактическая база

К моменту подготовки `v1` в репозитории уже зафиксированы следующие foundations:

- local MVP на Java 21, Spring Boot, PostgreSQL, React, Vite и TypeScript;
- truthful ledger: accounts и transactions;
- CSV import с идемпотентным поведением;
- calm dashboard;
- базовые Peace metrics: net worth, monthly burn и monthly savings;
- roadmap milestones `7-12`, формирующие путь к closed beta.

## 4. Принятые допущения

- `v1` не является public launch и трактуется как invited closed beta;
- релизная граница проходит по milestone `7-12`;
- Russian FIRE в `v1` поставляется как explainable scenario engine, а не как юридическая или налоговая консультация;
- для richer imports достаточно одного нового формата сверх CSV при наличии preview и rules flow;
- для аутентификации достаточно email/password и server-side sessions при условии надежной изоляции данных.

## 5. Состав версии v1

### 5.1. Базовая foundation, обязательная к сохранению

- truthful ledger: accounts и transactions;
- CSV import с идемпотентным поведением;
- calm dashboard;
- базовые Peace metrics;
- clean architecture boundaries вокруг domain truth.

### 5.2. Входит в состав `v1`

- безопасное изменение и удаление счетов и транзакций;
- recurring transactions и fixed-amount templates;
- goals и planning signals;
- richer imports с preview и deterministic normalization rules;
- Russian FIRE scenario modeling в пределах явно описанных assumptions;
- closed beta delivery: домен, аутентификация, user-level isolation, deploy path, backup и smoke checks.

### 5.3. Не входит в состав `v1`

- open signup и growth funnel;
- bank sync и live brokerage integrations;
- OAuth/social login;
- mobile apps;
- ML categorization;
- notification-heavy engagement loops;
- multi-country tax coverage;
- advanced platform complexity beyond needs of the first closed beta.

## 6. Релизные ограничения

Версия `v1` не должна трактоваться как полный публичный SaaS или универсальная personal finance platform. Ее назначение состоит в проверке того, что truthful money data, calm review и explainable planning действительно образуют работоспособную closed beta для пользователей, ориентированных на долгосрочную финансовую устойчивость.
