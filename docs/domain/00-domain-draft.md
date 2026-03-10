# ТЗ-05. Финансовая доменная модель v1

**Статус:** рабочий проект  
**Назначение документа:** зафиксировать состав и смысловые роли ключевых сущностей финансовой модели Mindful Finance v1 для последующего проектирования schema v1, business logic и правил расчета.  
**Связь с Техническим заданием:** материал используется при подготовке разделов о финансовой модели, доменных границах, сущностях учета и проектных решениях уровня domain/application.  
**Источник в репозитории:** `docs/domain/00-domain-draft.md`

## 1. Назначение доменной модели

Финансовая доменная модель Mindful Finance v1 должна описывать, как система представляет денежную реальность, а не только интерфейсные сценарии. В модели необходимо явно разделять первичные учетные факты, справочные и valuation-входы, расчетные агрегаты и planning-сущности, чтобы код и схема данных не смешивали truth, forecast и explanation.

Целевой принцип `v1` состоит в том, что финансовая картина строится на основе `account-ledger first`. Источником истины считаются счета и факты движения стоимости, а портфель, позиции, стоимость активов и прогресс к FIRE выводятся из этих фактов на выбранную дату `asOf`.

## 2. Канонический принцип v1: `account-ledger first`

В `v1` первичный mutable truth не хранится в виде готовых состояний портфеля. Канонический контур модели задается следующими правилами:

1. первичный финансовый факт фиксируется на уровне `Account` и `Transaction`;
2. `Portfolio`, `CashBalance` и `Position` представляют собой расчетные агрегаты поверх ledger-фактов;
3. стоимость активов не хранится как произвольное "текущее значение позиции", а вычисляется на основе `PriceSnapshot`;
4. planning-уровень (`FIRE Goal`, `AnnualExpensesBasis`, `SafeWithdrawalRate`) не изменяет truth-ledger и не подменяет его;
5. implicit FX conversion не допускается: если системе нужен общий итог в одной валюте, это требует явного FX input.

Такой подход нужен для того, чтобы финансовое состояние можно было реконструировать на любую дату, а логика домена не зависела от UI-представлений и "сохраненных итогов".

## 3. Слои доменной модели

| Слой | Сущности | Роль в модели |
| --- | --- | --- |
| Ledger truth | `Account`, `Transaction` | Хранение первичных учетных фактов, из которых выводится состояние системы |
| Reference / valuation facts | `Asset`, `PriceSnapshot` | Справочные сведения об инструментах и наблюдаемые цены для оценки стоимости |
| Planning inputs | `AnnualExpensesBasis`, `SafeWithdrawalRate`, `FIRE Goal` | Явные planning-параметры и цели, не являющиеся фактами учета |
| Derived state | `CashBalance`, `Position`, `Portfolio` | Расчетное состояние на дату `asOf`, получаемое из truth и valuation inputs |

## 4. Ключевые сущности и смысловые роли

| Сущность | Категория | Назначение | Ключевые атрибуты v1 |
| --- | --- | --- | --- |
| `Account` | ledger truth | Контейнер для учетных фактов в одной расчетной валюте | `id`, `name`, `currency`, `type`, `status`, `createdAt` |
| `Transaction` | ledger truth | Первичный факт денежного или инвестиционного события | `id`, `type`, `accountId`, `occurredOn`, `settlementCurrency`, `cashAmount`, `assetId?`, `quantity?`, `unitPrice?`, `feeAmount?`, `memo?`, `createdAt` |
| `Asset` | reference fact | Справочная идентификация инструмента для позиций и valuation | `id`, `ticker`, `name`, `assetClass`, `quoteCurrency`, `status` |
| `PriceSnapshot` | valuation fact | Наблюдаемая цена инструмента на дату `asOf` из явного источника | `assetId`, `asOf`, `price`, `currency`, `source` |
| `CashBalance` | derived state | Расчетный остаток кэша по валюте на дату `asOf` | `scope`, `currency`, `amount`, `asOf` |
| `Position` | derived state | Расчетное владение активом в выбранном scope | `scope`, `assetId`, `quantity`, `acquisitionCost`, `asOf` |
| `Portfolio` | derived state | Агрегированное состояние по набору счетов на дату `asOf` | `scope`, `cashBalances`, `positions`, `valueByCurrency`, `asOf` |
| `AnnualExpensesBasis` | planning input | Явно выбранная база расходов для FIRE-расчетов | `basisKind`, `currency`, `amount`, `asOf`, `source`, `notes?` |
| `SafeWithdrawalRate` | planning input | Параметр вывода FIRE target из annual expenses | `ratio`, `source`, `effectiveFrom`, `notes?` |
| `FIRE Goal` | planning entity | Пользовательская цель финансовой независимости в заданном scope | `id`, `title`, `scope`, `calculationCurrency`, `annualExpensesBasisRef`, `safeWithdrawalRateRef`, `targetDate?`, `status` |

## 5. Связи между сущностями

Связи `v1` фиксируются следующим образом:

1. `Account` содержит ledger-факты `Transaction`; каждый `Transaction` принадлежит ровно одному `Account`.
2. `Transaction` может ссылаться на `Asset`, если событие связано с инвестиционной позицией (`buy`, `sell`, `dividend_cash`).
3. `PriceSnapshot` относится к одному `Asset` и используется только как valuation input; он не изменяет ledger сам по себе.
4. `Position` выводится из множества `Transaction` по выбранному `scope` и не хранится как независимый mutable факт.
5. `CashBalance` выводится из `Transaction` по валютам и счетам в выбранном `scope`.
6. `Portfolio` агрегирует `CashBalance`, `Position` и `PriceSnapshot` на дату `asOf`.
7. `FIRE Goal` связывает выбранный portfolio scope, расчетную валюту, `AnnualExpensesBasis` и `SafeWithdrawalRate`.

## 6. Жизненный цикл и доменные границы

Для `v1` принимаются следующие правила жизненного цикла:

1. `Account` может быть активным или архивным; архивирование не удаляет исторические факты.
2. `Transaction` является учетным фактом и после фиксации не должен молча превращаться в derived state; исправления должны быть аудируемыми.
3. `Asset` и `PriceSnapshot` относятся к reference/valuation-слою и не должны подменять truth-ledger.
4. `AnnualExpensesBasis`, `SafeWithdrawalRate` и `FIRE Goal` принадлежат planning-слою и не должны рассматриваться как "истина учета".
5. `Portfolio`, `Position` и `CashBalance` всегда вычисляются на дату `asOf`; сохранение их в виде кэша возможно только как техническая оптимизация, но не как канонический источник истины.

## 7. Границы v1 и явные non-goals

В `v1` из модели сознательно исключаются:

- short positions, leverage, margin debt и derivatives;
- implicit FX conversion и скрытые курсы пересчета;
- хранение "готовой стоимости портфеля" без исходных valuation inputs;
- смешение учета, прогноза и инвестиционной рекомендации;
- legal advice и полноценный tax engine.

Russian-specific правила уровня IIS/OFZ/NDFL рассматриваются как последующие расширения поверх этой модели. Базовый domain framing `v1` должен быть достаточно строгим для generic-core, но не превращаться в налоговый движок.
