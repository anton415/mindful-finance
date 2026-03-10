# ТЗ-09. Определения `fact / plan / scenario / estimate`

**Статус:** рабочий проект  
**Назначение документа:** формализовать различие между `fact`, `plan`, `scenario` и `estimate` в модели Mindful Finance v1 и зафиксировать правила их хранения, смешивания и применения в расчетах.  
**Связь с Техническим заданием:** материал используется при проектировании schema v1, классификации сущностей и отделении truth-логики от planning/scenario-слоев.  
**Источник в репозитории:** `docs/domain/04-model-definitions.md`

## 1. Базовые определения

### 1.1 `fact`

`Fact` - это наблюдаемый или зафиксированный учетный факт, относящийся к реальному состоянию системы. Факт существует независимо от того, хочет ли пользователь его видеть, и должен позволять реконструировать состояние на дату `asOf`.

### 1.2 `plan`

`Plan` - это намерение пользователя совершить контролируемое будущее действие. План относится к событиям, на которые пользователь или система могут сознательно влиять, например плановым пополнениям, покупкам или выводам.

### 1.3 `scenario`

`Scenario` - это гипотетический набор событий и параметров для моделирования "что если". Сценарий может использовать те же типы операций, что и fact-слой, но не изменяет truth-ledger и должен быть вычислительно изолирован.

### 1.4 `estimate`

`Estimate` - это оценочное числовое предположение или параметр, который нужен для planning/scenario-расчетов, но не является фактом учета. К estimate относятся, например, `planning_annual_expenses` и `SafeWithdrawalRate`.

## 2. Правила хранения

Для `v1` действуют следующие правила хранения:

1. `fact` хранится в truth-ledger или factual reference store и не должен молча переписываться estimate-значением;
2. `plan` хранится отдельно от fact, чтобы будущие намерения не загрязняли учетную реальность;
3. `scenario` хранится отдельно по идентификатору сценария и всегда вычисляется поверх неизменного набора fact;
4. `estimate` хранится как параметр с указанием источника, даты действия и области применения;
5. derived metrics (`portfolio value`, `progress to FIRE`) не хранятся как `fact`, если их можно восстановить из входов.

## 3. Правила смешивания в расчетах

Смешивание слоев допускается только по явным правилам:

1. truth-view использует только `fact` и factual valuation inputs;
2. planning-view использует `fact + plan + estimate`;
3. scenario-view использует `fact + scenario + estimate`;
4. никакой view не должен silently fallback с отсутствующего fact на plan или estimate;
5. каждый расчетный output должен позволять ответить, какие слои данных в него вошли.

Следовательно, если metric использует estimate или scenario input, это должно быть явно отмечено в данных и в пользовательском объяснении результата.

## 4. Классификация сущностей

| Сущность / результат | Класс в модели `v1` | Комментарий |
| --- | --- | --- |
| `Account` | `fact` | Структурный учетный факт портфельного контура |
| `Transaction` | `fact` | Первичный факт ledger-события |
| `Asset` | `fact` | Справочный factual registry |
| `PriceSnapshot` | `fact` | Наблюдаемая valuation price на дату `asOf`; future assumptions не относятся к `PriceSnapshot` |
| `CashBalance` | `derived` | Вычисляется из fact-операций |
| `Position` | `derived` | Вычисляется из fact-операций и asset reference |
| `Portfolio state` | `derived` | Агрегат поверх truth-ledger |
| `Portfolio value` | `derived` | Результат расчета из `CashBalance`, `Position`, `PriceSnapshot` и при необходимости FX inputs |
| `realized_annual_expenses_ttm` | `derived` | Вычисляется из расходных fact-операций |
| `planning_annual_expenses` | `estimate` | Planning input |
| `AnnualExpensesBasis` | `derived` или `estimate` | Класс зависит от `basisKind`: `REALIZED_TTM` или `PLANNING_MANUAL` |
| `SafeWithdrawalRate` | `estimate` | Явный planning-параметр |
| `FIRE Goal` | `plan` | Цель и конфигурация planning-расчета |
| `progress_to_fire` | `derived` | Вычисляемый metric, зависящий от выбранного basis |

## 5. Классификация операций и событий

| Операция / событие | Базовый класс | Допустимые альтернативные классы | Правило применения |
| --- | --- | --- | --- |
| `cash_contribution` | `fact` | `plan`, `scenario` | В truth-слое отражает совершенное пополнение; в planning/scenario-слое моделируется отдельно |
| `cash_withdrawal` | `fact` | `plan`, `scenario` | Truth-вывод и planning/scenario-вывод должны храниться раздельно |
| `buy` | `fact` | `plan`, `scenario` | Не допускается использовать planned buy как truth-позицию |
| `sell` | `fact` | `plan`, `scenario` | Не допускается использовать scenario sell для уменьшения factual quantity |
| `dividend_cash` | `fact` | `scenario` | Будущие дивиденды являются прогнозным cashflow, а не plan-событием под контролем пользователя |
| `fee` | `fact` | `scenario` | Ожидаемые комиссии относятся к сценарному моделированию, а не к truth до их наступления |
| `dividend_reinvestment` | составное событие | `fact`, `scenario` | Класс наследуется от дочерних событий `dividend_cash + buy`; atomic record не допускается |

## 6. Практические следствия для schema v1 и business logic

Из настоящих определений следуют обязательные ограничения:

1. schema v1 должна позволять явно различать truth-ledger, planning и scenario данные;
2. `AnnualExpensesBasis` должен хранить не только amount, но и basis/source;
3. `SafeWithdrawalRate` должен быть отдельным planning-параметром, а не "зашитой константой" без источника;
4. расчеты уровня `progress_to_fire` должны возвращать traceable explanation: какая база расходов использована, какой `SWR` применен, требовался ли FX input;
5. ни один сервис не должен автоматически превращать derived metric в новый факт учета.
