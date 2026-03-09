# ТЗ-07. Доменные правила и ограничения

**Статус:** рабочий проект  
**Назначение документа:** зафиксировать доменные инварианты, допустимые типы финансовых операций и правила их влияния на состояние портфеля, стоимость активов и FIRE-метрики в модели Mindful Finance v1.  
**Связь с Техническим заданием:** материал используется при подготовке разделов о business rules, schema constraints, доменных сервисах и validation policy.  
**Источник в репозитории:** `docs/domain/02-business-rules.md`

## 1. Базовые инварианты модели

Для `v1` фиксируются следующие базовые инварианты:

1. деньги представлены как `BigDecimal` с явной валютой; float/double для денежных расчетов не допускаются;
2. каждый `Account` имеет одну расчетную валюту; implicit FX conversion внутри счета и между счетами не допускается;
3. каждый `Transaction` относится ровно к одному `Account`, имеет тип, дату `occurredOn` и положительные числовые значения для количеств и денежных полей;
4. каждая комиссия должна быть отражена явно в полях операции или отдельным `fee`-событием; скрытые комиссии в "грязной" сумме не допускаются;
5. `Position.quantity` не может быть отрицательной; short positions и leverage в `v1` не поддерживаются;
6. `PriceSnapshot.price` должен быть положительным и иметь явную валюту и источник;
7. `SafeWithdrawalRate.ratio` хранится как дробный коэффициент `0 < ratio < 1`;
8. `AnnualExpensesBasis` обязан явно указывать, является ли база расходов фактической (`REALIZED_TTM`) или плановой (`PLANNING_MANUAL`);
9. truth-ledger, planning и scenario-слой не смешиваются без явной маркировки model class.

## 2. Правила по сущностям

### 2.1 `Account`

- активный счет может принимать новые fact-операции;
- архивный счет сохраняет историю, но по умолчанию не используется для новых операций;
- валюта счета является settlement currency по умолчанию для его cash-событий.

### 2.2 `Asset`

- актив идентифицируется независимо от счета;
- справочник активов не хранит позицию пользователя;
- quote currency актива должна быть явно указана для valuation.

### 2.3 `Transaction`

- тип операции задает обязательный набор атрибутов;
- одна операция не может одновременно быть `fact` и `scenario`;
- для trade-операций quantities, unit prices, gross cash amount и fees должны быть восстановимы без догадок;
- `dividend_reinvestment` не хранится как атомарный transaction type.

## 3. Матрица допустимых операций

| Операция | Обязательные атрибуты | Допустимые model classes | Влияние на cash | Влияние на position quantity | Влияние на portfolio state / calculations |
| --- | --- | --- | --- | --- | --- |
| `cash_contribution` | `accountId`, `occurredOn`, `settlementCurrency`, `cashAmount > 0` | `fact`, `plan`, `scenario` | `+ cashAmount` | нет | Увеличивает cash и portfolio value; не считается investment return; в FIRE влияет только через investable value |
| `cash_withdrawal` | `accountId`, `occurredOn`, `settlementCurrency`, `cashAmount > 0`, `expenseEligibility` | `fact`, `plan`, `scenario` | `- cashAmount` | нет | Уменьшает cash и portfolio value; не считается investment return; в `realized_annual_expenses_ttm` входит только при `expenseEligibility = INCLUDED` |
| `buy` | `accountId`, `assetId`, `occurredOn`, `settlementCurrency`, `quantity > 0`, `unitPrice > 0`, `grossCashAmount > 0`, `feeAmount >= 0` | `fact`, `plan`, `scenario` | `- grossCashAmount - feeAmount` | `+ quantity` | Увеличивает позицию, уменьшает cash; увеличивает acquisition cost на `gross + fee`; сам по себе не является investment return и не входит в annual expenses |
| `sell` | `accountId`, `assetId`, `occurredOn`, `settlementCurrency`, `quantity > 0`, `unitPrice > 0`, `grossCashAmount > 0`, `feeAmount >= 0` | `fact`, `plan`, `scenario` | `+ grossCashAmount - feeAmount` | `- quantity` | Уменьшает позицию и увеличивает cash на net proceeds; отрицательная позиция запрещена; сам по себе не является annual expenses |
| `dividend_cash` | `accountId`, `assetId`, `occurredOn`, `settlementCurrency`, `cashAmount > 0` | `fact`, `scenario` | `+ cashAmount` | нет | Увеличивает cash и portfolio value; считается investment return; не считается contribution |
| `fee` | `accountId`, `occurredOn`, `settlementCurrency`, `cashAmount > 0`, `feeKind` | `fact`, `scenario` | `- cashAmount` | нет | Уменьшает cash и portfolio value; если комиссия относится к trade, ее эффект должен учитываться в cost/proceeds; по умолчанию не включается в living-expenses basis FIRE |
| `dividend_reinvestment` | Полный набор атрибутов дочерних событий `dividend_cash` и `buy`, связанных одним business event id | `fact`, `scenario` | Нет отдельного atomic cash effect: сумма эффектов дочерних событий | Увеличивает quantity через дочерний `buy` | Моделируется только как композиция `dividend_cash + buy`; отдельный transaction type в truth-ledger запрещен |

## 4. Правила учета комиссий, дивидендов, пополнений и выводов

### 4.1 Комиссии

Для комиссий принимаются следующие правила:

1. комиссия всегда отображается явно;
2. комиссия по `buy` увеличивает acquisition cost позиции и дополнительно уменьшает cash;
3. комиссия по `sell` уменьшает net proceeds и уменьшает cash relative to gross proceeds;
4. standalone `fee` учитывается как самостоятельный outflow;
5. комиссия не может быть спрятана в `unitPrice` или потеряна при импорте.

### 4.2 Дивиденды

Для дивидендов принимаются следующие правила:

1. `dividend_cash` увеличивает cash в валюте выплаты;
2. дивиденд считается инвестиционным доходом, а не пополнением;
3. реинвестирование дивиденда отражается только через композицию `dividend_cash + buy`;
4. ожидание будущих дивидендов относится к scenario- или estimate-слою, но не к truth-ledger.

### 4.3 Пополнения и выводы

Для пополнений и выводов принимаются следующие правила:

1. `cash_contribution` и `cash_withdrawal` изменяют funding портфельного контура, но не инвестиционную доходность;
2. `cash_withdrawal` может участвовать в `realized_annual_expenses_ttm` только при явной расходной квалификации;
3. движение капитала между truth и external world не должно автоматически трактоваться как расход или доходность.

## 5. Что хранится как факт, а что вычисляется

Как первичный факт или reference input в `v1` допускается хранить:

- `Account`;
- `Transaction`;
- `Asset`;
- `PriceSnapshot`.

Как derived state или metric в `v1` должны вычисляться:

- `CashBalance`;
- `Position`;
- `Portfolio state`;
- `Portfolio value`;
- `realized_annual_expenses_ttm`;
- `fire_target`;
- `progress_to_fire`.

Как planning-параметры или цели в `v1` допускается хранить отдельно от truth-ledger:

- `AnnualExpensesBasis`;
- `SafeWithdrawalRate`;
- `FIRE Goal`.

## 6. Явные доменные границы

В рамках данного блока не считаются допустимым поведением системы:

- смешение fact-операций с planned/scenario-events в одном расчете без явной маркировки источника;
- автоматическое построение единой currency total без FX input;
- implicit price carry-forward из future snapshot;
- подмена фактических annual expenses оценочными значениями без явного выбора basis;
- вывод инвестиционных рекомендаций из `safe withdrawal rate`.
