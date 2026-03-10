import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import type {
  CreatePersonalFinanceCardRequest,
  PersonalExpenseCategoryCode,
  PersonalExpenseCategoryDto,
  PersonalFinanceCardDto,
  PersonalFinanceIncomeForecastDto,
  PersonalFinanceSnapshotDto,
  UpdateMonthlyExpenseRequest,
  UpdateMonthlyIncomeActualRequest,
  UpdatePersonalFinanceSettingsRequest,
} from '../../api'

type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
export type PersonalFinanceTab = 'expenses' | 'income' | 'settings'
export interface PersonalFinanceCardListItem extends PersonalFinanceCardDto {
  currentBalance: string | null
  currency: string | null
}

interface PersonalFinanceViewProps {
  status: LoadStatus
  cards: PersonalFinanceCardListItem[]
  snapshot: PersonalFinanceSnapshotDto | null
  selectedCardId: string | null
  activeTab: PersonalFinanceTab
  year: number
  errorMessage: string | null
  onSelectTab: (tab: PersonalFinanceTab) => void
  onSelectYear: (year: number) => void
  onSelectCard: (cardId: string) => void
  onRetry: () => void
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveExpenseActual: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
  onSaveIncomeActual: (month: number, request: UpdateMonthlyIncomeActualRequest) => Promise<boolean>
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
}

const MONTH_LABELS = [
  'Январь',
  'Февраль',
  'Март',
  'Апрель',
  'Май',
  'Июнь',
  'Июль',
  'Август',
  'Сентябрь',
  'Октябрь',
  'Ноябрь',
  'Декабрь',
] as const

export function PersonalFinanceView({
  status,
  cards,
  snapshot,
  selectedCardId,
  activeTab,
  year,
  errorMessage,
  onSelectTab,
  onSelectYear,
  onSelectCard,
  onRetry,
  onCreateCard,
  onSaveExpenseActual,
  onSaveIncomeActual,
  onSaveSettings,
}: PersonalFinanceViewProps) {
  if (status === 'loading' || status === 'idle') {
    return <InlineStatus tone="neutral" message="Загружаем личные финансы..." />
  }

  if (status === 'error') {
    return (
      <InlineStatus
        tone="warning"
        message={errorMessage ?? 'Не удалось загрузить личные финансы.'}
        actionLabel="Повторить"
        onAction={onRetry}
      />
    )
  }

  const hasCards = cards.length > 0
  const selectedCard = snapshot?.card ?? cards.find((card) => card.id === selectedCardId) ?? null
  const yearOptions = selectableYearOptions()

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-slate-50/70 p-4 lg:p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">Личные финансы</h2>
            <p className="mt-1 max-w-3xl text-sm text-slate-600">
              Один связанный cash ledger на карту: факт расходов и доходов меняет баланс,
              лимиты и прогноз живут в настройках и повторяются каждый месяц одинаково.
            </p>
          </div>

          <label className="w-full text-sm text-slate-600 sm:max-w-52">
            Год
            <select
              value={year}
              onChange={(event) => onSelectYear(Number(event.target.value))}
              disabled={!hasCards}
              className={selectClassName()}
            >
              {yearOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>

        <PersonalFinanceCardSelector
          cards={cards}
          selectedCardId={selectedCard?.id ?? null}
          onSelectCard={onSelectCard}
        />
      </section>

      <section className="rounded-3xl border border-slate-200 bg-slate-50/70 p-3">
        <nav className="inline-flex rounded-2xl border border-slate-200 bg-white p-1">
          <NestedTabButton
            label="Расходы"
            isActive={activeTab === 'expenses'}
            disabled={!hasCards}
            onClick={() => onSelectTab('expenses')}
          />
          <NestedTabButton
            label="Доходы"
            isActive={activeTab === 'income'}
            disabled={!hasCards}
            onClick={() => onSelectTab('income')}
          />
          <NestedTabButton
            label="Настройки"
            isActive={activeTab === 'settings'}
            onClick={() => onSelectTab('settings')}
          />
        </nav>
      </section>

      {!hasCards ? (
        <PersonalFinanceSettingsEmptyState onCreateCard={onCreateCard} />
      ) : !snapshot ? (
        <InlineStatus tone="neutral" message="Выберите карту, чтобы загрузить данные." />
      ) : activeTab === 'expenses' ? (
        <ExpensesTab
          key={`expenses-${snapshot.card.id}-${snapshot.year}`}
          snapshot={snapshot}
          onSaveExpenseActual={onSaveExpenseActual}
        />
      ) : activeTab === 'income' ? (
        <IncomeTab
          key={`income-${snapshot.card.id}-${snapshot.year}`}
          snapshot={snapshot}
          onSaveIncomeActual={onSaveIncomeActual}
        />
      ) : (
        <SettingsTab
          key={`settings-${snapshot.card.id}-${snapshot.year}-${snapshot.settings.currentBalance}`}
          snapshot={snapshot}
          onCreateCard={onCreateCard}
          onSaveSettings={onSaveSettings}
        />
      )}
    </div>
  )
}

interface ExpensesTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onSaveExpenseActual: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
}

function ExpensesTab({ snapshot, onSaveExpenseActual }: ExpensesTabProps) {
  const [selectedMonth, setSelectedMonth] = useState<number>(() => defaultActualMonth(snapshot.year))
  const [isExpenseDrawerOpen, setIsExpenseDrawerOpen] = useState<boolean>(false)
  const selectedMonthData =
    snapshot.expenses.months.find((month) => month.month === selectedMonth) ?? snapshot.expenses.months[0]

  return (
    <div className="space-y-4">
      <section className="rounded-3xl border border-slate-200 bg-white p-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-base font-semibold text-slate-900">Фактические расходы</h3>
            <p className="mt-1 text-sm text-slate-600">
              Добавляйте расходы через панель справа, не теряя из виду годовую таблицу.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setIsExpenseDrawerOpen(true)}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white"
          >
            + Добавить расходы
          </button>
        </div>
      </section>

      {isExpenseDrawerOpen ? (
        <DrawerShell
          title="Фактические расходы"
          description="Выберите месяц и сохраните факт по категориям. Лимиты остаются в заголовках годовой таблицы и в настройках."
          onClose={() => setIsExpenseDrawerOpen(false)}
        >
          <ExpenseEntryFormCard
            key={`actual-${snapshot.card.id}-${snapshot.year}-${selectedMonth}-${serializeExpenseMonth(selectedMonthData.actualCategoryAmounts, snapshot.categories)}`}
            title="Расходы по категориям"
            description="Лимиты здесь не задаются. Они живут на вкладке настроек и повторяются одинаково каждый месяц."
            submitLabel="Сохранить факт"
            selectedMonth={selectedMonth}
            onSelectMonth={setSelectedMonth}
            year={snapshot.year}
            currency={snapshot.currency}
            categories={snapshot.categories}
            initialValues={selectedMonthData.actualCategoryAmounts}
            onSave={onSaveExpenseActual}
            onSaved={() => setIsExpenseDrawerOpen(false)}
          />
        </DrawerShell>
      ) : null}

      <ExpenseReviewTable snapshot={snapshot} />
    </div>
  )
}

interface IncomeTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onSaveIncomeActual: (month: number, request: UpdateMonthlyIncomeActualRequest) => Promise<boolean>
}

function IncomeTab({ snapshot, onSaveIncomeActual }: IncomeTabProps) {
  const [selectedMonth, setSelectedMonth] = useState<number>(() => defaultActualMonth(snapshot.year))
  const selectedMonthData =
    snapshot.income.months.find((month) => month.month === selectedMonth) ?? snapshot.income.months[0]

  return (
    <div className="space-y-4">
      <IncomeActualFormCard
        key={`actual-income-${snapshot.card.id}-${snapshot.year}-${selectedMonth}-${selectedMonthData.totalAmount}-${selectedMonthData.status ?? 'EMPTY'}`}
        year={snapshot.year}
        currency={snapshot.currency}
        selectedMonth={selectedMonth}
        onSelectMonth={setSelectedMonth}
        initialAmount={selectedMonthData.status === 'ACTUAL' ? selectedMonthData.totalAmount : ''}
        onSave={onSaveIncomeActual}
      />

      <RecurringIncomeSummaryCard
        currency={snapshot.currency}
        forecast={snapshot.settings.incomeForecast}
      />

      <IncomeReviewTable snapshot={snapshot} />
    </div>
  )
}

interface SettingsTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
}

function SettingsTab({ snapshot, onCreateCard, onSaveSettings }: SettingsTabProps) {
  return (
    <div className="space-y-4">
      <CreateCardSection onCreateCard={onCreateCard} hasExistingCards />
      <SettingsDetails snapshot={snapshot} onSaveSettings={onSaveSettings} />
    </div>
  )
}

function SettingsDetails({
  snapshot,
  onSaveSettings,
}: {
  snapshot: PersonalFinanceSnapshotDto
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
}) {
  const [baselineAmount, setBaselineAmount] = useState<string>(
    toDraftAmount(snapshot.settings.baselineAmount),
  )
  const [salaryAmount, setSalaryAmount] = useState<string>(
    toDraftAmount(snapshot.settings.incomeForecast?.salaryAmount ?? '0.00'),
  )
  const [bonusPercent, setBonusPercent] = useState<string>(
    toDraftAmount(snapshot.settings.incomeForecast?.bonusPercent ?? '0.00'),
  )
  const [limitValues, setLimitValues] = useState<Record<PersonalExpenseCategoryCode, string>>(() =>
    toExpenseDraftValues(snapshot.settings.recurringLimitCategoryAmounts, snapshot.categories),
  )
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const areBaseValuesValid =
    isValidNonNegativeAmountValue(baselineAmount) &&
    isValidNonNegativeAmountValue(salaryAmount) &&
    isValidNonNegativeAmountValue(bonusPercent)
  const areLimitsValid = snapshot.categories.every((category) =>
    isValidNonNegativeAmountValue(limitValues[category.code]),
  )
  const canSave = areBaseValuesValid && areLimitsValid && status !== 'submitting'
  const recurringLimitTotal = sumDecimalAmountStrings(
    snapshot.categories.map((category) => toDecimalAmountString(limitValues[category.code])),
  )
  const monthlyForecast = calculateForecastAmount(salaryAmount, bonusPercent)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSave) {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)

    const saved = await onSaveSettings({
      baselineAmount: toDecimalAmountString(baselineAmount),
      limitCategoryAmounts: snapshot.categories.reduce(
        (result, category) => ({
          ...result,
          [category.code]: toDecimalAmountString(limitValues[category.code]),
        }),
        {} as Record<PersonalExpenseCategoryCode, string>,
      ),
      salaryAmount: toDecimalAmountString(salaryAmount),
      bonusPercent: toDecimalAmountString(bonusPercent),
    })

    if (saved) {
      setStatus('idle')
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить настройки.')
  }

  return (
    <>
      <section className="grid gap-4 lg:grid-cols-3">
        <MetricTile
          label="Текущий баланс карты"
          value={formatAmountWithCurrency(snapshot.settings.currentBalance, snapshot.currency)}
          hint="Считается из baseline и фактических доходов/расходов."
        />
        <MetricTile
          label="Связанный счёт"
          value={snapshot.settings.linkedAccountId}
          hint="Этот cash account виден в Инвестициях, но редактируется только отсюда."
        />
        <MetricTile
          label="Повторяющийся лимит"
          value={formatAmountWithCurrency(recurringLimitTotal, snapshot.currency)}
          hint="Одинаковый месячный лимит по всем категориям."
        />
      </section>

      <form
        className="space-y-4 rounded-3xl border border-slate-200 bg-white p-4"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <div>
          <h3 className="text-base font-semibold text-slate-900">Настройки карты</h3>
          <p className="mt-1 text-sm text-slate-600">
            Эти значения задаются один раз и переиспользуются для каждого месяца любого выбранного года.
          </p>
        </div>

        <section className="rounded-2xl bg-slate-50 p-4">
          <div className="grid gap-3 md:grid-cols-2">
            <label className="text-sm text-slate-600">
              Начальный баланс карты
              <input
                type="text"
                inputMode="decimal"
                value={baselineAmount}
                onChange={(event) => setBaselineAmount(normalizeAmountInput(event.target.value))}
                placeholder="0.00"
                className={inputClassName(isValidNonNegativeAmountValue(baselineAmount))}
              />
            </label>

            <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Сейчас на карте</p>
              <p className="mt-1 text-lg font-semibold text-slate-900">
                {formatAmountWithCurrency(snapshot.settings.currentBalance, snapshot.currency)}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                Меняется от фактических monthly income и expense записей.
              </p>
            </div>
          </div>
        </section>

        <section className="rounded-2xl bg-slate-50 p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Лимиты по категориям</h4>
              <p className="mt-1 text-sm text-slate-600">
                Эти лимиты попадают в каждую строку годовой таблицы расходов.
              </p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Итого лимит</p>
              <p className="mt-1 text-sm font-semibold text-slate-900">
                {formatAmountWithCurrency(recurringLimitTotal, snapshot.currency)}
              </p>
            </div>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {snapshot.categories.map((category) => (
              <label key={category.code} className="text-sm text-slate-600">
                {category.label}
                <input
                  type="text"
                  inputMode="decimal"
                  value={limitValues[category.code]}
                  onChange={(event) =>
                    setLimitValues((current) => ({
                      ...current,
                      [category.code]: normalizeAmountInput(event.target.value),
                    }))
                  }
                  placeholder="0.00"
                  className={inputClassName(isValidNonNegativeAmountValue(limitValues[category.code]))}
                />
              </label>
            ))}
          </div>
        </section>

        <section className="rounded-2xl bg-slate-50 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Прогноз доходов</h4>
              <p className="mt-1 text-sm text-slate-600">
                Оклад заполняется рублями, премия заполняется процентами. Прогноз используется только
                там, где нет факта.
              </p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Прогноз в месяц</p>
              <p className="mt-1 text-sm font-semibold text-slate-900">
                {formatAmountWithCurrency(monthlyForecast, snapshot.currency)}
              </p>
            </div>
          </div>

          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <label className="text-sm text-slate-600">
              Оклад (RUB)
              <input
                type="text"
                inputMode="decimal"
                value={salaryAmount}
                onChange={(event) => setSalaryAmount(normalizeAmountInput(event.target.value))}
                placeholder="0.00"
                className={inputClassName(isValidNonNegativeAmountValue(salaryAmount))}
              />
            </label>

            <label className="text-sm text-slate-600">
              Премия (%)
              <input
                type="text"
                inputMode="decimal"
                value={bonusPercent}
                onChange={(event) => setBonusPercent(normalizeAmountInput(event.target.value))}
                placeholder="0.00"
                className={inputClassName(isValidNonNegativeAmountValue(bonusPercent))}
              />
            </label>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <MetricTile
              label="Оклад"
              value={formatAmountWithCurrency(toDecimalAmountString(salaryAmount), snapshot.currency)}
            />
            <MetricTile
              label="Премия"
              value={`${formatAmount(toDecimalAmountString(bonusPercent))}%`}
            />
            <MetricTile
              label="Расчётная премия"
              value={formatAmountWithCurrency(calculateBonusAmount(salaryAmount, bonusPercent), snapshot.currency)}
            />
          </div>
        </section>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <div className="text-sm text-slate-600">
            Только фактические доходы и расходы двигают баланс linked account.
          </div>
          <button
            type="submit"
            disabled={!canSave}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {status === 'submitting' ? 'Сохраняем...' : 'Сохранить настройки'}
          </button>
        </div>

        {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
        {!areBaseValuesValid || !areLimitsValid ? (
          <p className="text-xs text-rose-600">Разрешены только неотрицательные значения с 2 знаками.</p>
        ) : null}
      </form>
    </>
  )
}

function PersonalFinanceSettingsEmptyState({
  onCreateCard,
}: {
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
}) {
  return (
    <div className="space-y-4">
      <CreateCardSection onCreateCard={onCreateCard} hasExistingCards={false} />
      <InlineStatus
        tone="neutral"
        message="Создайте первую карту в настройках, чтобы затем вести расходы, доходы и годовой обзор."
      />
    </div>
  )
}

function CreateCardSection({
  onCreateCard,
  hasExistingCards,
}: {
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
  hasExistingCards: boolean
}) {
  const [newCardName, setNewCardName] = useState<string>('')
  const [createCardStatus, setCreateCardStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [createCardErrorMessage, setCreateCardErrorMessage] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (newCardName.trim().length === 0 || createCardStatus === 'submitting') {
      return
    }

    setCreateCardStatus('submitting')
    setCreateCardErrorMessage(null)

    const created = await onCreateCard({ name: newCardName.trim() })
    if (created) {
      setNewCardName('')
      setCreateCardStatus('idle')
      return
    }

    setCreateCardStatus('error')
    setCreateCardErrorMessage('Не удалось добавить карту.')
  }

  return (
    <section
      className={`rounded-3xl border p-4 ${
        hasExistingCards ? 'border-slate-200 bg-slate-50/70' : 'border-slate-200 bg-white'
      }`}
    >
      <div className="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h3 className="text-base font-semibold text-slate-900">
            {hasExistingCards ? 'Добавить ещё одну карту' : 'Добавить первую карту'}
          </h3>
          <p className="mt-1 text-sm text-slate-600">
            {hasExistingCards
              ? 'Создание карты спрятано в настройки, потому что это редкое действие.'
              : 'Сначала создайте карту, а затем настройте баланс, лимиты и прогноз доходов.'}
          </p>
        </div>
      </div>

      <form
        className="mt-4 flex flex-col gap-2 sm:flex-row sm:items-start"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <label className="w-full text-sm text-slate-600">
          Название карты
          <input
            type="text"
            value={newCardName}
            onChange={(event) => setNewCardName(event.target.value)}
            placeholder="Например, T-Банк Black"
            className={inputClassName(true)}
          />
        </label>
        <div className="flex sm:pt-6">
          <button
            type="submit"
            disabled={newCardName.trim().length === 0 || createCardStatus === 'submitting'}
            className="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300 sm:w-auto"
          >
            {createCardStatus === 'submitting' ? 'Добавляем...' : 'Добавить'}
          </button>
        </div>
      </form>

      {createCardErrorMessage ? (
        <p className="mt-3 text-xs text-rose-600">{createCardErrorMessage}</p>
      ) : null}
    </section>
  )
}

function RecurringIncomeSummaryCard({
  currency,
  forecast,
}: {
  currency: string
  forecast: PersonalFinanceIncomeForecastDto | null
}) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <h3 className="text-base font-semibold text-slate-900">Повторяющийся прогноз доходов</h3>
      {forecast ? (
        <div className="mt-3 grid gap-3 md:grid-cols-4">
          <MetricTile
            label="Оклад"
            value={formatAmountWithCurrency(forecast.salaryAmount, currency)}
          />
          <MetricTile label="Премия" value={`${formatAmount(forecast.bonusPercent)}%`} />
          <MetricTile
            label="Премия в рублях"
            value={formatAmountWithCurrency(forecast.bonusAmount, currency)}
          />
          <MetricTile
            label="Прогноз в месяц"
            value={formatAmountWithCurrency(forecast.totalAmount, currency)}
          />
        </div>
      ) : (
        <p className="mt-2 text-sm text-slate-600">
          Прогноз ещё не задан. Его можно настроить на вкладке настроек.
        </p>
      )}
    </section>
  )
}

function PersonalFinanceCardSelector({
  cards,
  selectedCardId,
  onSelectCard,
}: {
  cards: PersonalFinanceCardListItem[]
  selectedCardId: string | null
  onSelectCard: (cardId: string) => void
}) {
  return (
    <article className="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Карты личных финансов</h3>
          <p className="mt-1 text-xs text-slate-500">
            Выберите карту, чтобы открыть расходы, доходы и настройки.
          </p>
        </div>
      </div>

      {cards.length === 0 ? (
        <div className="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
          Пока нет карт. Создание доступно на вкладке настроек.
        </div>
      ) : (
        <ul className="mt-4 space-y-2">
          {cards.map((card) => (
            <li key={card.id}>
              <button
                type="button"
                onClick={() => onSelectCard(card.id)}
                className={`w-full rounded-lg border px-3 py-3 text-left transition ${
                  selectedCardId === card.id
                    ? 'border-slate-300 bg-slate-50 shadow-sm'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/70'
                }`}
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-slate-900">{card.name}</p>
                    <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                      Наличные · Личные финансы
                    </p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-xs uppercase tracking-wide text-slate-500">
                      {card.currency ?? 'RUB'}
                    </p>
                    <p className="mt-1 text-sm font-semibold text-slate-900">
                      {card.currentBalance ? formatAmount(card.currentBalance) : '...'}
                    </p>
                  </div>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}

function MetricTile({
  label,
  value,
  hint,
}: {
  label: string
  value: string
  hint?: string
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
      <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">{label}</p>
      <p className="mt-1 break-all text-sm font-semibold text-slate-900">{value}</p>
      {hint ? <p className="mt-1 text-xs text-slate-500">{hint}</p> : null}
    </div>
  )
}

interface ExpenseEntryFormCardProps {
  title: string
  description: string
  submitLabel: string
  selectedMonth: number
  onSelectMonth: (month: number) => void
  year: number
  currency: string
  categories: PersonalExpenseCategoryDto[]
  initialValues: Record<PersonalExpenseCategoryCode, string>
  onSave: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
  onSaved?: () => void
}

function ExpenseEntryFormCard({
  title,
  description,
  submitLabel,
  selectedMonth,
  onSelectMonth,
  year,
  currency,
  categories,
  initialValues,
  onSave,
  onSaved,
}: ExpenseEntryFormCardProps) {
  const [draftValues, setDraftValues] = useState<Record<PersonalExpenseCategoryCode, string>>(() =>
    toExpenseDraftValues(initialValues, categories),
  )
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const hasInvalidValues = categories.some((category) => !isValidNonNegativeAmountValue(draftValues[category.code]))
  const total = sumDecimalAmountStrings(
    categories.map((category) => toDecimalAmountString(draftValues[category.code])),
  )
  const canSave = !hasInvalidValues && status !== 'submitting'

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSave) {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)

    const saved = await onSave(selectedMonth, {
      year,
      categoryAmounts: categories.reduce(
        (result, category) => ({
          ...result,
          [category.code]: toDecimalAmountString(draftValues[category.code]),
        }),
        {} as Record<PersonalExpenseCategoryCode, string>,
      ),
    })

    if (saved) {
      setStatus('idle')
      onSaved?.()
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить фактические расходы.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <p className="mt-1 text-sm text-slate-600">{description}</p>
        </div>
        <label className="w-full text-sm text-slate-600 sm:max-w-64">
          Месяц
          <select
            value={selectedMonth}
            onChange={(event) => onSelectMonth(Number(event.target.value))}
            className={selectClassName()}
          >
            {MONTH_LABELS.map((label, index) => (
              <option key={label} value={index + 1}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <form
        className="mt-4 space-y-3"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {categories.map((category) => (
            <label key={category.code} className="text-sm text-slate-600">
              {category.label}
              <input
                type="text"
                inputMode="decimal"
                value={draftValues[category.code]}
                onChange={(event) =>
                  setDraftValues((current) => ({
                    ...current,
                    [category.code]: normalizeAmountInput(event.target.value),
                  }))
                }
                placeholder="0.00"
                className={inputClassName(isValidNonNegativeAmountValue(draftValues[category.code]))}
              />
            </label>
          ))}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <div>
            <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Итого</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">
              {formatAmountWithCurrency(total, currency)}
            </p>
          </div>
          <button
            type="submit"
            disabled={!canSave}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {status === 'submitting' ? 'Сохраняем...' : submitLabel}
          </button>
        </div>

        {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
        {hasInvalidValues ? (
          <p className="text-xs text-rose-600">Разрешены только неотрицательные суммы с 2 знаками.</p>
        ) : null}
      </form>
    </section>
  )
}

function DrawerShell({
  title,
  description,
  onClose,
  children,
}: {
  title: string
  description: string
  onClose: () => void
  children: ReactNode
}) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-end bg-slate-950/35 sm:items-stretch sm:justify-end">
      <button
        type="button"
        aria-label="Закрыть панель"
        className="absolute inset-0 cursor-default"
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative z-10 flex max-h-[92vh] w-full flex-col overflow-hidden rounded-t-3xl bg-white shadow-2xl sm:h-full sm:max-h-none sm:max-w-2xl sm:rounded-none sm:rounded-l-3xl"
      >
        <div className="border-b border-slate-200 px-5 py-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
              <p className="mt-1 text-sm text-slate-600">{description}</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700"
            >
              Закрыть
            </button>
          </div>
        </div>
        <div className="flex-1 overflow-y-auto p-5">{children}</div>
      </aside>
    </div>
  )
}

function ExpenseReviewTable({ snapshot }: { snapshot: PersonalFinanceSnapshotDto }) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица расходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          Факт вводится по месяцам, а лимит повторяется из настроек для каждой строки одинаково.
        </p>
      </div>

      <div className="lg:hidden">
        <div className="space-y-3 p-4">
          {snapshot.expenses.months.map((month) => (
            <article key={month.month} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h4 className="text-sm font-semibold text-slate-900">{toMonthLabel(month.month)}</h4>
                  <p className="mt-1 text-xs text-slate-500">
                    Факт {formatAmount(month.actualTotal)} · Лимит {formatAmount(month.limitTotal)}
                  </p>
                </div>
              </div>

              <div className="mt-4 space-y-2">
                {snapshot.categories.map((category) => {
                  const actual = month.actualCategoryAmounts[category.code]
                  const limit = month.limitCategoryAmounts[category.code]
                  const isOverLimit = isPositiveAmount(limit) && compareDecimalStrings(actual, limit) > 0

                  return (
                    <div
                      key={category.code}
                      className={`rounded-2xl border px-3 py-2 ${
                        isOverLimit
                          ? 'border-rose-200 bg-rose-50'
                          : 'border-slate-200 bg-white'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className="text-sm font-medium text-slate-900">{category.label}</p>
                        <p className={`text-sm font-semibold ${isOverLimit ? 'text-rose-700' : 'text-slate-900'}`}>
                          {formatAmountOrDash(actual)}
                        </p>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">Лимит {formatAmountOrDash(limit)}</p>
                    </div>
                  )
                })}
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <MetricTile label="Факт за месяц" value={formatAmount(month.actualTotal)} />
                <MetricTile label="Лимит за месяц" value={formatAmount(month.limitTotal)} />
              </div>
            </article>
          ))}

          <section className="rounded-2xl border border-slate-200 bg-white p-4">
            <h4 className="text-sm font-semibold text-slate-900">Итоги года</h4>
            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              {snapshot.categories.map((category) => (
                <div key={category.code} className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2">
                  <p className="text-sm font-medium text-slate-900">{category.label}</p>
                  <p className="mt-2 text-xs text-slate-500">
                    Факт {formatAmount(snapshot.expenses.actualTotalsByCategory[category.code])}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Лимит {formatAmount(snapshot.expenses.limitTotalsByCategory[category.code])}
                  </p>
                </div>
              ))}
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              <MetricTile
                label="Годовой факт"
                value={formatAmountWithCurrency(snapshot.expenses.annualActualTotal, snapshot.currency)}
              />
              <MetricTile
                label="Годовой лимит"
                value={formatAmountWithCurrency(snapshot.expenses.annualLimitTotal, snapshot.currency)}
              />
              <MetricTile
                label="Средний факт"
                value={formatAmountWithCurrency(snapshot.expenses.averageMonthlyActualTotal, snapshot.currency)}
              />
            </div>
          </section>
        </div>
      </div>

      <div className="hidden lg:block">
        <table className="w-full border-collapse text-[11px] leading-4 xl:text-[12px]">
          <thead className="bg-slate-50">
            <tr>
              <th className="w-20 border-b border-r border-slate-200 px-2 py-3 text-left font-semibold text-slate-900 xl:w-24 xl:px-3">
                Месяц
              </th>
              {snapshot.categories.map((category) => (
                <th
                  key={category.code}
                  className="border-b border-r border-slate-200 px-3 py-3 text-left text-slate-900"
                >
                  <span className="block whitespace-nowrap font-semibold">{category.label}</span>
                  <span className="mt-1 block whitespace-nowrap text-[11px] font-medium text-slate-500">
                    Лимит {formatAmountOrDash(snapshot.settings.recurringLimitCategoryAmounts[category.code])}
                  </span>
                </th>
              ))}
              <th className="w-20 border-b border-r border-slate-200 px-2 py-3 text-left font-semibold text-slate-900 xl:w-24">
                Факт
              </th>
              <th className="w-20 border-b border-slate-200 px-2 py-3 text-left font-semibold text-slate-900 xl:w-24">
                Лимит
              </th>
            </tr>
          </thead>
          <tbody>
            {snapshot.expenses.months.map((month) => (
              <tr key={month.month} className="align-top">
                <td className="border-b border-r border-slate-200 px-2 py-3 font-semibold text-slate-900 xl:px-3">
                  {toMonthLabel(month.month)}
                </td>
                {snapshot.categories.map((category) => {
                  const actual = month.actualCategoryAmounts[category.code]
                  const limit = month.limitCategoryAmounts[category.code]
                  const isOverLimit = isPositiveAmount(limit) && compareDecimalStrings(actual, limit) > 0

                  return (
                    <td
                      key={category.code}
                      className={`border-b border-r border-slate-200 px-2 py-2 ${
                        isOverLimit ? 'bg-rose-50' : ''
                      }`}
                    >
                      <p className={`font-semibold ${isOverLimit ? 'text-rose-700' : 'text-slate-900'}`}>
                        {formatAmountOrDash(actual)}
                      </p>
                    </td>
                  )
                })}
                <td className="border-b border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                  {formatAmount(month.actualTotal)}
                </td>
                <td className="border-b border-slate-200 px-2 py-3 font-semibold text-slate-900">
                  {formatAmount(month.limitTotal)}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-slate-50">
            <tr>
              <td className="border-t border-r border-slate-200 px-3 py-3 font-semibold text-slate-900">
                Годовой факт
              </td>
              {snapshot.categories.map((category) => (
                <td key={category.code} className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                  {formatAmount(snapshot.expenses.actualTotalsByCategory[category.code])}
                </td>
              ))}
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(snapshot.expenses.annualActualTotal)}
              </td>
              <td className="border-t border-slate-200 px-2 py-3 text-slate-500">Факт за год</td>
            </tr>
            <tr>
              <td className="border-t border-r border-slate-200 px-3 py-3 font-semibold text-slate-900">
                Годовой лимит
              </td>
              {snapshot.categories.map((category) => (
                <td key={category.code} className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                  {formatAmount(snapshot.expenses.limitTotalsByCategory[category.code])}
                </td>
              ))}
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(snapshot.expenses.annualLimitTotal)}
              </td>
              <td className="border-t border-slate-200 px-2 py-3 text-slate-500">Лимит за год</td>
            </tr>
            <tr>
              <td
                colSpan={snapshot.categories.length + 1}
                className="border-t border-r border-slate-200 px-3 py-3 text-sm text-slate-600"
              >
                Средний факт по заполненным месяцам
              </td>
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(snapshot.expenses.averageMonthlyActualTotal)}
              </td>
              <td className="border-t border-slate-200 px-2 py-3 text-slate-500">Среднее</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  )
}

interface IncomeActualFormCardProps {
  year: number
  currency: string
  selectedMonth: number
  onSelectMonth: (month: number) => void
  initialAmount: string
  onSave: (month: number, request: UpdateMonthlyIncomeActualRequest) => Promise<boolean>
}

function IncomeActualFormCard({
  year,
  currency,
  selectedMonth,
  onSelectMonth,
  initialAmount,
  onSave,
}: IncomeActualFormCardProps) {
  const [amount, setAmount] = useState<string>(normalizeAmountInput(initialAmount))
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const isValid = isValidNonNegativeAmountValue(amount)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!isValid || status === 'submitting') {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)
    const saved = await onSave(selectedMonth, {
      year,
      totalAmount: toDecimalAmountString(amount),
    })

    if (saved) {
      setStatus('idle')
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить фактический доход.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h3 className="text-base font-semibold text-slate-900">Фактический доход</h3>
          <p className="mt-1 text-sm text-slate-600">
            Сохраняется только итоговая сумма за выбранный месяц.
          </p>
        </div>
      </div>

      <form
        className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,240px)_minmax(0,1fr)_auto]"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <label className="text-sm text-slate-600">
          Месяц
          <select
            value={selectedMonth}
            onChange={(event) => onSelectMonth(Number(event.target.value))}
            className={selectClassName()}
          >
            {MONTH_LABELS.map((label, index) => (
              <option key={label} value={index + 1}>
                {label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm text-slate-600">
          Итоговый доход
          <input
            type="text"
            inputMode="decimal"
            value={amount}
            onChange={(event) => setAmount(normalizeAmountInput(event.target.value))}
            placeholder="0.00"
            className={inputClassName(isValid)}
          />
        </label>

        <div className="flex items-end">
          <button
            type="submit"
            disabled={!isValid || status === 'submitting'}
            className="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300 lg:w-auto"
          >
            {status === 'submitting' ? 'Сохраняем...' : 'Сохранить факт'}
          </button>
        </div>
      </form>

      <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3">
        <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Сумма</p>
        <p className="mt-1 text-lg font-semibold text-slate-900">
          {formatAmountWithCurrency(toDecimalAmountString(amount), currency)}
        </p>
      </div>

      {errorMessage ? <p className="mt-3 text-xs text-rose-600">{errorMessage}</p> : null}
      {!isValid ? (
        <p className="mt-3 text-xs text-rose-600">Разрешены только неотрицательные суммы с 2 знаками.</p>
      ) : null}
    </section>
  )
}

function IncomeReviewTable({ snapshot }: { snapshot: PersonalFinanceSnapshotDto }) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица доходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          Если факт не задан, строка заполняется повторяющимся прогнозом из настроек.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[520px] border-collapse text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="border-b border-r border-slate-200 px-4 py-3 text-left font-semibold text-slate-900">
                Месяц
              </th>
              <th className="border-b border-r border-slate-200 px-4 py-3 text-left font-semibold text-slate-900">
                Доход
              </th>
              <th className="border-b border-slate-200 px-4 py-3 text-left font-semibold text-slate-900">
                Статус
              </th>
            </tr>
          </thead>
          <tbody>
            {snapshot.income.months.map((month) => (
              <tr key={month.month}>
                <td className="border-b border-r border-slate-200 px-4 py-3 font-medium text-slate-900">
                  {toMonthLabel(month.month)}
                </td>
                <td className="border-b border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                  {formatAmountOrDash(month.totalAmount)}
                </td>
                <td className="border-b border-slate-200 px-4 py-3">
                  {month.status ? <StatusBadge status={month.status} /> : <span className="text-slate-400">-</span>}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-slate-50">
            <tr>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                Годовой итог
              </td>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                {formatAmount(snapshot.income.annualTotal)}
              </td>
              <td className="border-t border-slate-200 px-4 py-3 text-slate-500">Факт + прогноз</td>
            </tr>
            <tr>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                Среднее
              </td>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                {formatAmount(snapshot.income.averageMonthlyTotal)}
              </td>
              <td className="border-t border-slate-200 px-4 py-3 text-slate-500">По всем месяцам года</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  )
}

function StatusBadge({ status }: { status: 'ACTUAL' | 'FORECAST' }) {
  if (status === 'ACTUAL') {
    return (
      <span className="inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
        Факт
      </span>
    )
  }

  return (
    <span className="inline-flex rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
      Прогноз
    </span>
  )
}

function NestedTabButton({
  label,
  isActive,
  disabled = false,
  onClick,
}: {
  label: string
  isActive: boolean
  disabled?: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`rounded-xl px-4 py-2 text-sm font-medium transition ${
        isActive
          ? 'bg-slate-900 text-white shadow-sm'
          : disabled
            ? 'cursor-not-allowed text-slate-300'
            : 'text-slate-600 hover:text-slate-900'
      }`}
    >
      {label}
    </button>
  )
}

function InlineStatus({
  tone,
  message,
  actionLabel,
  onAction,
}: {
  tone: 'neutral' | 'warning'
  message: string
  actionLabel?: string
  onAction?: () => void
}) {
  return (
    <div
      className={`rounded-3xl border px-5 py-4 text-sm ${
        tone === 'warning'
          ? 'border-amber-300 bg-amber-50 text-amber-900'
          : 'border-slate-200 bg-slate-50 text-slate-700'
      }`}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p>{message}</p>
        {actionLabel && onAction ? (
          <button
            type="button"
            onClick={onAction}
            className="rounded-xl border border-current px-3 py-1.5 text-xs font-semibold"
          >
            {actionLabel}
          </button>
        ) : null}
      </div>
    </div>
  )
}

function selectClassName(): string {
  return controlClassName(true)
}

function inputClassName(isValid: boolean): string {
  return controlClassName(isValid)
}

function controlClassName(isValid: boolean): string {
  return `mt-1 block h-11 w-full rounded-xl border bg-white px-3 text-sm outline-none disabled:cursor-not-allowed disabled:bg-slate-100 ${
    isValid ? 'border-slate-200 text-slate-900' : 'border-rose-300 text-rose-700'
  }`
}

function toMonthLabel(month: number): string {
  return MONTH_LABELS[month - 1] ?? String(month)
}

function defaultActualMonth(year: number): number {
  return year === currentYear() ? currentMonthNumber() : 1
}

function currentYear(): number {
  return new Date().getFullYear()
}

function currentMonthNumber(): number {
  return new Date().getMonth() + 1
}

function selectableYearOptions(): number[] {
  const current = currentYear()
  return Array.from({ length: 7 }, (_, index) => current - 3 + index)
}

function serializeExpenseMonth(
  values: Record<PersonalExpenseCategoryCode, string>,
  categories: PersonalExpenseCategoryDto[],
): string {
  return categories.map((category) => values[category.code] ?? '0.00').join('|')
}

function toExpenseDraftValues(
  values: Record<PersonalExpenseCategoryCode, string>,
  categories: PersonalExpenseCategoryDto[],
): Record<PersonalExpenseCategoryCode, string> {
  return categories.reduce(
    (result, category) => ({
      ...result,
      [category.code]: toDraftAmount(values[category.code]),
    }),
    {} as Record<PersonalExpenseCategoryCode, string>,
  )
}

function toDraftAmount(value: string): string {
  return value === '0.00' ? '' : value
}

function normalizeAmountInput(value: string): string {
  return value.replace(',', '.').replace(/[^\d.]/g, '')
}

function isValidNonNegativeAmountValue(value: string): boolean {
  const trimmed = value.trim()
  if (trimmed.length === 0) {
    return true
  }
  return /^\d+(\.\d{0,2})?$/.test(trimmed)
}

function toDecimalAmountString(value: string): string {
  const trimmed = value.trim()
  if (trimmed.length === 0 || !isValidNonNegativeAmountValue(trimmed)) {
    return '0.00'
  }

  const parsed = Number.parseFloat(trimmed)
  if (!Number.isFinite(parsed) || parsed < 0) {
    return '0.00'
  }

  return parsed.toFixed(2)
}

function sumDecimalAmountStrings(values: string[]): string {
  const total = values.reduce((sum, value) => sum + Number.parseFloat(value || '0'), 0)
  return total.toFixed(2)
}

function calculateBonusAmount(salaryAmount: string, bonusPercent: string): string {
  const salary = Number.parseFloat(toDecimalAmountString(salaryAmount))
  const bonus = Number.parseFloat(toDecimalAmountString(bonusPercent))
  return ((salary * bonus) / 100).toFixed(2)
}

function calculateForecastAmount(salaryAmount: string, bonusPercent: string): string {
  return sumDecimalAmountStrings([
    toDecimalAmountString(salaryAmount),
    calculateBonusAmount(salaryAmount, bonusPercent),
  ])
}

function formatAmount(value: string): string {
  const parsed = Number.parseFloat(value)
  if (!Number.isFinite(parsed)) {
    return value
  }

  return new Intl.NumberFormat('ru-RU', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(parsed)
}

function formatAmountWithCurrency(value: string, currency: string): string {
  return `${formatAmount(value)} ${currency}`
}

function formatAmountOrDash(value: string): string {
  return isPositiveAmount(value) ? formatAmount(value) : '-'
}


function isPositiveAmount(value: string): boolean {
  return compareDecimalStrings(value, '0.00') > 0
}

function compareDecimalStrings(left: string, right: string): number {
  return Number.parseFloat(left || '0') - Number.parseFloat(right || '0')
}
