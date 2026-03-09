# ТЗ-08. Правила расчётов портфеля и FIRE

**Статус:** рабочий проект  
**Назначение документа:** пошагово зафиксировать правила расчета `portfolio state`, `portfolio value`, annual expenses basis и `progress to FIRE` в модели Mindful Finance v1.  
**Связь с Техническим заданием:** материал используется при проектировании domain/application services, формул, тест-кейсов и acceptance rules для финансовой логики.  
**Источник в репозитории:** `docs/domain/03-calculation-rules.md`

## 1. Общий принцип расчета

Все расчетные показатели `v1` строятся на дату `asOf` из явно выбранного набора входов. Базовый набор входов включает:

1. portfolio scope как множество счетов, входящих в расчет;
2. fact-транзакции с `occurredOn <= asOf`;
3. valuation inputs уровня `PriceSnapshot`;
4. при необходимости - явные FX inputs для пересчета в одну расчетную валюту;
5. planning inputs уровня `AnnualExpensesBasis` и `SafeWithdrawalRate`.

Ни один расчет не должен использовать неявные курсы, скрытые price assumptions или автоматически подменять отсутствующий truth planning-параметром.

## 2. Расчет `portfolio_state(asOf)`

`portfolio_state(asOf)` строится в следующем порядке:

1. определяется portfolio scope;
2. из truth-ledger выбираются только `fact`-операции, относящиеся к этому scope и удовлетворяющие условию `occurredOn <= asOf`;
3. fact-операции группируются на cash events и asset-related events;
4. по cash events вычисляются `cash_balance(asOf)` по валютам;
5. по asset-related events вычисляются `position.quantity(asOf)` и агрегированная acquisition cost;
6. результатом является `portfolio state`, содержащий cash balances, positions, признаки неполной valuation и служебную информацию о примененных inputs.

Если часть активов не имеет допустимого `PriceSnapshot`, это не ломает `portfolio_state`, но делает `portfolio_value` неполным. Система не должна молча заменять отсутствующую цену на ноль.

## 3. Расчет `position.quantity(asOf)`

Для каждого `assetId` в выбранном scope:

```text
position.quantity(asOf)
  = sum(buy.quantity)
  + sum(dividend_reinvestment.buy.quantity)
  - sum(sell.quantity)
```

где в сумму попадают только fact-события с `occurredOn <= asOf`.

Дополнительно действуют следующие правила:

1. `dividend_reinvestment` влияет на quantity только через дочерний `buy`;
2. результат не может быть отрицательным;
3. lot-level tax accounting и выбор метода cost basis в `v1` не фиксируются; для целей состояния портфеля достаточно aggregate quantity и aggregate acquisition cost.

## 4. Расчет `cash_balance(asOf)`

Для каждой валюты в выбранном scope:

```text
cash_balance(asOf)
  = sum(cash_contribution.cashAmount)
  + sum(sell.grossCashAmount)
  + sum(dividend_cash.cashAmount)
  - sum(cash_withdrawal.cashAmount)
  - sum(buy.grossCashAmount)
  - sum(all fee amounts)
```

Правила уточнения:

1. комиссия по `buy` и `sell` всегда вычитается отдельно и явно;
2. при `dividend_reinvestment` cash-эффект определяется суммой дочерних событий;
3. если в scope присутствуют несколько валют, `cash_balance` возвращается как валютная карта, а не как один total.

## 5. Расчет `asset_value(asOf)` и `portfolio_value(asOf)`

### 5.1 Выбор `PriceSnapshot`

Для каждого актива выбирается последний доступный factual `PriceSnapshot`, удовлетворяющий условию:

```text
snapshot.asOf <= valuationAsOf
```

Если подходящий snapshot отсутствует, стоимость позиции считается `unresolved`, а не нулевой.

### 5.2 Расчет `asset_value(asOf)`

```text
asset_value(asOf) = position.quantity(asOf) * selected_price_snapshot.price
```

Валюта результата определяется валютой выбранного `PriceSnapshot`.

### 5.3 Расчет `portfolio_value(asOf)`

```text
portfolio_value(asOf)
  = cash balances by currency
  + sum(asset_value(asOf)) by currency
```

Базовый результат `v1` представляет собой набор totals по валютам. Если требуется общий итог в одной расчетной валюте, действует правило:

1. расчетная валюта выбирается явно;
2. все non-native currency buckets конвертируются только при наличии явного FX input;
3. без FX input единый total не строится.

Таким образом, multi-currency truth является базовым поведением модели, а не ошибкой.

## 6. Расчет annual expenses basis

В `v1` различаются две сущности:

### 6.1 `realized_annual_expenses_ttm`

`realized_annual_expenses_ttm(asOf)` определяется как сумма fact-операций, признанных расходными, за trailing 12 months:

```text
occurredOn in (asOf.minusYears(1), asOf]
```

В `v1` в этот metric включаются только `cash_withdrawal`, помеченные как `expenseEligibility = INCLUDED`. Из расчета исключаются:

- `cash_contribution`;
- `buy`;
- `sell`;
- `dividend_cash`;
- комиссии и прочие портфельные friction costs, если они не были явно перенесены в spending basis отдельным бизнес-правилом.

### 6.2 `planning_annual_expenses`

`planning_annual_expenses` является estimate input и задается явно пользователем или product rule. Он не выводится автоматически из truth, если пользователь или сценарий явно не выбрали `REALIZED_TTM` как basis.

Следовательно, bare-term `annual expenses` в FIRE-расчете допустим только после указания basis:

- `REALIZED_TTM`;
- `PLANNING_MANUAL`.

## 7. Расчет `fire_target` и `progress_to_fire`

### 7.1 Предусловия

Для расчета `progress_to_fire` должны быть явно заданы:

1. portfolio scope;
2. расчетная валюта;
3. `AnnualExpensesBasis`;
4. `SafeWithdrawalRate`;
5. при multi-currency portfolio - FX inputs, если часть investable value выражена в иных валютах.

### 7.2 Формулы

```text
fire_target = annual_expenses / swr
```

где:

- `annual_expenses` - значение из выбранного `AnnualExpensesBasis`;
- `swr` - `SafeWithdrawalRate.ratio`.

```text
progress_to_fire = investable_portfolio_value / fire_target
```

Дополнительные правила:

1. `investable_portfolio_value` вычисляется в расчетной валюте из `portfolio_value(asOf)` с явным FX input при необходимости;
2. raw progress не ограничивается 100%; ограничение или визуальная каппировка может существовать только на UI-уровне;
3. `SafeWithdrawalRate` не должен равняться нулю или превышать 1;
4. если отсутствует выбранная база расходов или расчетная валюта не может быть получена из-за отсутствия FX inputs, `progress_to_fire` не вычисляется.

## 8. Контрольные расчетные сценарии

Для ручной проверки доменной логики должны использоваться как минимум следующие сценарии:

1. `buy + fee`  
   Покупка на `1_000 RUB` с комиссией `5 RUB` должна уменьшать cash на `1_005 RUB`, увеличивать quantity и увеличивать acquisition cost на `1_005 RUB`.
2. `sell + fee`  
   Продажа на `480 RUB` с комиссией `3 RUB` должна увеличивать cash на `477 RUB` и уменьшать quantity без перехода позиции в отрицательное значение.
3. `cash dividend`  
   Дивиденд `50 RUB` должен увеличить cash на `50 RUB`, не меняя quantity и не считаясь contribution.
4. `dividend reinvestment`  
   Событие реинвестирования должно быть представлено как `dividend_cash + buy`; отдельный atomic type не допускается.
5. `multi-currency portfolio without FX`  
   При наличии `RUB` и `USD` bucket результатом `portfolio_value` должен быть набор totals по валютам без единого суммарного числа.
6. `FIRE progress with planning annual expenses + SWR`  
   При `investable_portfolio_value = 12_000_000 RUB`, `planning_annual_expenses = 480_000 RUB` и `swr = 0.04` система должна вычислить `fire_target = 12_000_000 RUB` и `progress_to_fire = 1.0`.
