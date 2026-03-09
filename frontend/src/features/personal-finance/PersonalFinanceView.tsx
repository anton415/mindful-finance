import { useState, type FormEvent } from 'react'
import type {
  CreatePersonalFinanceCardRequest,
  PersonalExpenseCategoryCode,
  PersonalExpenseCategoryDto,
  PersonalFinanceCardDto,
  PersonalFinanceSnapshotDto,
  UpdateIncomeForecastRequest,
  UpdateMonthlyExpenseRequest,
  UpdateMonthlyIncomeActualRequest,
} from '../../api'

type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
export type PersonalFinanceTab = 'expenses' | 'income'

interface PersonalFinanceViewProps {
  status: LoadStatus
  cards: PersonalFinanceCardDto[]
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
  onSaveExpenseLimit: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
  onSaveIncomeActual: (month: number, request: UpdateMonthlyIncomeActualRequest) => Promise<boolean>
  onSaveIncomeForecast: (request: UpdateIncomeForecastRequest) => Promise<boolean>
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
  onSaveExpenseLimit,
  onSaveIncomeActual,
  onSaveIncomeForecast,
}: PersonalFinanceViewProps) {
  const [newCardName, setNewCardName] = useState<string>('')
  const [createCardStatus, setCreateCardStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [createCardErrorMessage, setCreateCardErrorMessage] = useState<string | null>(null)

  const handleCreateCardSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
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

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-slate-50/70 p-4 lg:p-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div className="space-y-3">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Личные финансы</h2>
              <p className="mt-1 max-w-3xl text-sm text-slate-600">
                Годовой manual review по выбранной карте: фактические расходы, лимиты, фактический
                доход и прогноз на будущие месяцы.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <label className="min-w-64 text-sm text-slate-600">
                Карта
                <select
                  value={selectedCard?.id ?? ''}
                  onChange={(event) => onSelectCard(event.target.value)}
                  disabled={!hasCards}
                  className="mt-1 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-100"
                >
                  {hasCards ? (
                    cards.map((card) => (
                      <option key={card.id} value={card.id}>
                        {card.name}
                      </option>
                    ))
                  ) : (
                    <option value="">Сначала добавьте карту</option>
                  )}
                </select>
              </label>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => onSelectYear(year - 1)}
                  className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700"
                >
                  Назад
                </button>
                <div className="min-w-24 rounded-xl border border-slate-200 bg-white px-4 py-2 text-center text-sm font-semibold text-slate-900">
                  {year}
                </div>
                <button
                  type="button"
                  onClick={() => onSelectYear(year + 1)}
                  className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700"
                >
                  Вперёд
                </button>
              </div>
            </div>
          </div>

          <form
            className="rounded-2xl border border-slate-200 bg-white p-3 sm:min-w-80"
            onSubmit={(event) => {
              void handleCreateCardSubmit(event)
            }}
          >
            <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-500">
              Добавить карту
            </p>
            <div className="mt-3 flex flex-col gap-2 sm:flex-row">
              <input
                type="text"
                value={newCardName}
                onChange={(event) => setNewCardName(event.target.value)}
                placeholder="Например, T-Банк Black"
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none"
              />
              <button
                type="submit"
                disabled={newCardName.trim().length === 0 || createCardStatus === 'submitting'}
                className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
              >
                {createCardStatus === 'submitting' ? 'Добавляем...' : 'Добавить'}
              </button>
            </div>
            {createCardErrorMessage ? (
              <p className="mt-2 text-xs text-rose-600">{createCardErrorMessage}</p>
            ) : null}
          </form>
        </div>

        <nav className="mt-4 inline-flex rounded-2xl border border-slate-200 bg-white p-1">
          <NestedTabButton
            label="Расходы"
            isActive={activeTab === 'expenses'}
            onClick={() => onSelectTab('expenses')}
          />
          <NestedTabButton
            label="Доходы"
            isActive={activeTab === 'income'}
            onClick={() => onSelectTab('income')}
          />
        </nav>
      </section>

      {!hasCards ? (
        <InlineStatus
          tone="neutral"
          message="Добавьте первую карту, чтобы вести yearly review расходов и доходов."
        />
      ) : !snapshot ? (
        <InlineStatus tone="neutral" message="Выберите карту, чтобы загрузить данные." />
      ) : activeTab === 'expenses' ? (
        <ExpensesTab
          key={`expenses-${snapshot.card.id}-${snapshot.year}`}
          snapshot={snapshot}
          onSaveExpenseActual={onSaveExpenseActual}
          onSaveExpenseLimit={onSaveExpenseLimit}
        />
      ) : (
        <IncomeTab
          key={`income-${snapshot.card.id}-${snapshot.year}`}
          snapshot={snapshot}
          onSaveIncomeActual={onSaveIncomeActual}
          onSaveIncomeForecast={onSaveIncomeForecast}
        />
      )}
    </div>
  )
}

interface ExpensesTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onSaveExpenseActual: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
  onSaveExpenseLimit: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
}

function ExpensesTab({ snapshot, onSaveExpenseActual, onSaveExpenseLimit }: ExpensesTabProps) {
  const [selectedMonth, setSelectedMonth] = useState<number>(() => defaultActualMonth(snapshot.year))

  const selectedMonthData =
    snapshot.expenses.months.find((month) => month.month === selectedMonth) ?? snapshot.expenses.months[0]

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,720px)_minmax(0,1fr)]">
      <section className="space-y-4">
        <MonthSelectorCard
          title="Месяц для ввода"
          description="Факт расходов и лимиты сохраняются отдельно, но всегда для одного и того же месяца."
          selectedMonth={selectedMonth}
          onSelectMonth={setSelectedMonth}
        />

        <div className="grid gap-4 lg:grid-cols-2">
          <ExpenseEntryFormCard
            key={`actual-${snapshot.card.id}-${snapshot.year}-${selectedMonth}-${serializeExpenseMonth(selectedMonthData.actualCategoryAmounts, snapshot.categories)}`}
            title="Фактические расходы"
            description="Введите итоговые суммы по категориям из банковского приложения за месяц."
            submitLabel="Сохранить факт"
            month={selectedMonth}
            year={snapshot.year}
            currency={snapshot.currency}
            categories={snapshot.categories}
            initialValues={selectedMonthData.actualCategoryAmounts}
            onSave={onSaveExpenseActual}
          />

          <ExpenseEntryFormCard
            key={`limit-${snapshot.card.id}-${snapshot.year}-${selectedMonth}-${serializeExpenseMonth(selectedMonthData.limitCategoryAmounts, snapshot.categories)}`}
            title="Лимиты по категориям"
            description="Лимиты помогают сразу видеть перерасход. Нули очищают месяц."
            submitLabel="Сохранить лимиты"
            month={selectedMonth}
            year={snapshot.year}
            currency={snapshot.currency}
            categories={snapshot.categories}
            initialValues={selectedMonthData.limitCategoryAmounts}
            onSave={onSaveExpenseLimit}
          />
        </div>
      </section>

      <ExpenseReviewTable snapshot={snapshot} />
    </div>
  )
}

interface IncomeTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onSaveIncomeActual: (month: number, request: UpdateMonthlyIncomeActualRequest) => Promise<boolean>
  onSaveIncomeForecast: (request: UpdateIncomeForecastRequest) => Promise<boolean>
}

function IncomeTab({ snapshot, onSaveIncomeActual, onSaveIncomeForecast }: IncomeTabProps) {
  const [selectedActualMonth, setSelectedActualMonth] = useState<number>(() => defaultActualMonth(snapshot.year))

  const selectedActualMonthData =
    snapshot.income.months.find((month) => month.month === selectedActualMonth) ?? snapshot.income.months[0]
  const forecastMonthOptions = forecastMonthNumbers(snapshot.year)

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,720px)_minmax(0,1fr)]">
      <section className="space-y-4">
        <IncomeActualFormCard
          key={`actual-income-${snapshot.card.id}-${snapshot.year}-${selectedActualMonth}-${selectedActualMonthData.totalAmount}-${selectedActualMonthData.status ?? 'EMPTY'}`}
          year={snapshot.year}
          currency={snapshot.currency}
          selectedMonth={selectedActualMonth}
          onSelectMonth={setSelectedActualMonth}
          initialAmount={selectedActualMonthData.status === 'ACTUAL' ? selectedActualMonthData.totalAmount : ''}
          onSave={onSaveIncomeActual}
        />

        <IncomeForecastFormCard
          key={`income-forecast-${snapshot.card.id}-${snapshot.year}-${snapshot.income.forecast?.startMonth ?? 'none'}-${snapshot.income.forecast?.salaryAmount ?? '0'}-${snapshot.income.forecast?.bonusAmount ?? '0'}`}
          year={snapshot.year}
          currency={snapshot.currency}
          monthOptions={forecastMonthOptions}
          initialStartMonth={resolveForecastStartMonth(snapshot.year, snapshot.income.forecast?.startMonth)}
          initialSalaryAmount={snapshot.income.forecast?.salaryAmount ?? ''}
          initialBonusAmount={snapshot.income.forecast?.bonusAmount ?? ''}
          onSave={onSaveIncomeForecast}
        />
      </section>

      <IncomeReviewTable snapshot={snapshot} />
    </div>
  )
}

interface MonthSelectorCardProps {
  title: string
  description: string
  selectedMonth: number
  onSelectMonth: (month: number) => void
}

function MonthSelectorCard({ title, description, selectedMonth, onSelectMonth }: MonthSelectorCardProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600">{description}</p>

      <label className="mt-4 block text-sm text-slate-600">
        Месяц
        <select
          value={selectedMonth}
          onChange={(event) => onSelectMonth(Number(event.target.value))}
          className="mt-1 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900"
        >
          {MONTH_LABELS.map((label, index) => (
            <option key={label} value={index + 1}>
              {label}
            </option>
          ))}
        </select>
      </label>
    </section>
  )
}

interface ExpenseEntryFormCardProps {
  title: string
  description: string
  submitLabel: string
  month: number
  year: number
  currency: string
  categories: PersonalExpenseCategoryDto[]
  initialValues: Record<PersonalExpenseCategoryCode, string>
  onSave: (month: number, request: UpdateMonthlyExpenseRequest) => Promise<boolean>
}

function ExpenseEntryFormCard({
  title,
  description,
  submitLabel,
  month,
  year,
  currency,
  categories,
  initialValues,
  onSave,
}: ExpenseEntryFormCardProps) {
  const [draftValues, setDraftValues] = useState<Record<PersonalExpenseCategoryCode, string>>(() =>
    toExpenseDraftValues(initialValues, categories),
  )
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const hasInvalidValues = categories.some((category) => !isValidNonNegativeAmountValue(draftValues[category.code]))
  const isDirty = categories.some(
    (category) => toDecimalAmountString(draftValues[category.code]) !== initialValues[category.code],
  )
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

    const saved = await onSave(month, {
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
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить форму.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <p className="mt-1 text-sm text-slate-600">{description}</p>
        </div>
        <div className="rounded-xl bg-slate-100 px-3 py-2 text-right">
          <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Месяц</p>
          <p className="mt-1 text-sm font-semibold text-slate-900">{toMonthLabel(month)}</p>
        </div>
      </div>

      <form
        className="mt-4 space-y-3"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <div className="grid gap-3 sm:grid-cols-2">
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
                className={`mt-1 block w-full rounded-xl border bg-white px-3 py-2.5 text-sm outline-none ${
                  isValidNonNegativeAmountValue(draftValues[category.code])
                    ? 'border-slate-200 text-slate-900'
                    : 'border-rose-300 text-rose-700'
                }`}
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
        {!isDirty ? (
          <p className="text-xs text-slate-500">Форма уже совпадает с сохранёнными значениями.</p>
        ) : null}
      </form>
    </section>
  )
}

interface ExpenseReviewTableProps {
  snapshot: PersonalFinanceSnapshotDto
}

function ExpenseReviewTable({ snapshot }: ExpenseReviewTableProps) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица расходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          В каждой ячейке показаны факт и лимит. Перерасход подсвечивается.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[1180px] table-fixed border-collapse text-[12px] leading-4">
          <thead className="bg-slate-50">
            <tr>
              <th className="w-28 border-b border-r border-slate-200 px-3 py-3 text-left font-semibold text-slate-900">
                Месяц
              </th>
              {snapshot.categories.map((category) => (
                <th
                  key={category.code}
                  className="border-b border-r border-slate-200 px-2 py-3 text-left font-semibold text-slate-900"
                >
                  {category.label}
                </th>
              ))}
              <th className="w-28 border-b border-r border-slate-200 px-2 py-3 text-left font-semibold text-slate-900">
                Факт
              </th>
              <th className="w-28 border-b border-slate-200 px-2 py-3 text-left font-semibold text-slate-900">
                Лимит
              </th>
            </tr>
          </thead>
          <tbody>
            {snapshot.expenses.months.map((month) => (
              <tr key={month.month} className="align-top">
                <td className="border-b border-r border-slate-200 px-3 py-3 font-semibold text-slate-900">
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
                      <p className="mt-1 text-[11px] text-slate-500">
                        Лимит {formatAmountOrDash(limit)}
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
      <h3 className="text-base font-semibold text-slate-900">Фактический доход</h3>
      <p className="mt-1 text-sm text-slate-600">
        По умолчанию форма открывает текущий месяц выбранного года и сохраняет только итоговую сумму.
      </p>

      <form
        className="mt-4 space-y-3"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <label className="block text-sm text-slate-600">
          Месяц
          <select
            value={selectedMonth}
            onChange={(event) => onSelectMonth(Number(event.target.value))}
            className="mt-1 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900"
          >
            {MONTH_LABELS.map((label, index) => (
              <option key={label} value={index + 1}>
                {label}
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm text-slate-600">
          Итоговый доход
          <input
            type="text"
            inputMode="decimal"
            value={amount}
            onChange={(event) => setAmount(normalizeAmountInput(event.target.value))}
            placeholder="0.00"
            className={`mt-1 block w-full rounded-xl border bg-white px-3 py-2.5 text-sm outline-none ${
              isValid ? 'border-slate-200 text-slate-900' : 'border-rose-300 text-rose-700'
            }`}
          />
        </label>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <div>
            <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Сумма</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">
              {formatAmountWithCurrency(toDecimalAmountString(amount), currency)}
            </p>
          </div>
          <button
            type="submit"
            disabled={!isValid || status === 'submitting'}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {status === 'submitting' ? 'Сохраняем...' : 'Сохранить факт'}
          </button>
        </div>

        {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
        {!isValid ? (
          <p className="text-xs text-rose-600">Разрешены только неотрицательные суммы с 2 знаками.</p>
        ) : null}
      </form>
    </section>
  )
}

interface IncomeForecastFormCardProps {
  year: number
  currency: string
  monthOptions: number[]
  initialStartMonth: number
  initialSalaryAmount: string
  initialBonusAmount: string
  onSave: (request: UpdateIncomeForecastRequest) => Promise<boolean>
}

function IncomeForecastFormCard({
  year,
  currency,
  monthOptions,
  initialStartMonth,
  initialSalaryAmount,
  initialBonusAmount,
  onSave,
}: IncomeForecastFormCardProps) {
  const [startMonth, setStartMonth] = useState<number>(initialStartMonth)
  const [salaryAmount, setSalaryAmount] = useState<string>(normalizeAmountInput(initialSalaryAmount))
  const [bonusAmount, setBonusAmount] = useState<string>(normalizeAmountInput(initialBonusAmount))
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const isSalaryValid = isValidNonNegativeAmountValue(salaryAmount)
  const isBonusValid = isValidNonNegativeAmountValue(bonusAmount)
  const totalAmount = sumDecimalAmountStrings([
    toDecimalAmountString(salaryAmount),
    toDecimalAmountString(bonusAmount),
  ])
  const canSave = isSalaryValid && isBonusValid && monthOptions.length > 0 && status !== 'submitting'

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSave) {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)
    const saved = await onSave({
      startMonth,
      salaryAmount: toDecimalAmountString(salaryAmount),
      bonusAmount: toDecimalAmountString(bonusAmount),
    })

    if (saved) {
      setStatus('idle')
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить прогноз доходов.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <h3 className="text-base font-semibold text-slate-900">Прогноз доходов</h3>
      <p className="mt-1 text-sm text-slate-600">
        Оклад и премия задают прогноз для будущих месяцев выбранного года. Фактические месяцы не
        затираются.
      </p>

      {monthOptions.length === 0 ? (
        <div className="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
          Для {year} года будущих месяцев больше нет. Переключите год, чтобы задать новый прогноз.
        </div>
      ) : (
        <form
          className="mt-4 space-y-3"
          onSubmit={(event) => {
            void handleSubmit(event)
          }}
        >
          <label className="block text-sm text-slate-600">
            Начать с месяца
            <select
              value={startMonth}
              onChange={(event) => setStartMonth(Number(event.target.value))}
              className="mt-1 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900"
            >
              {monthOptions.map((month) => (
                <option key={month} value={month}>
                  {toMonthLabel(month)}
                </option>
              ))}
            </select>
          </label>

          <div className="grid gap-3 sm:grid-cols-2">
            <label className="text-sm text-slate-600">
              Оклад
              <input
                type="text"
                inputMode="decimal"
                value={salaryAmount}
                onChange={(event) => setSalaryAmount(normalizeAmountInput(event.target.value))}
                placeholder="0.00"
                className={`mt-1 block w-full rounded-xl border bg-white px-3 py-2.5 text-sm outline-none ${
                  isSalaryValid ? 'border-slate-200 text-slate-900' : 'border-rose-300 text-rose-700'
                }`}
              />
            </label>

            <label className="text-sm text-slate-600">
              Премия
              <input
                type="text"
                inputMode="decimal"
                value={bonusAmount}
                onChange={(event) => setBonusAmount(normalizeAmountInput(event.target.value))}
                placeholder="0.00"
                className={`mt-1 block w-full rounded-xl border bg-white px-3 py-2.5 text-sm outline-none ${
                  isBonusValid ? 'border-slate-200 text-slate-900' : 'border-rose-300 text-rose-700'
                }`}
              />
            </label>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
            <div>
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Прогноз в месяц</p>
              <p className="mt-1 text-lg font-semibold text-slate-900">
                {formatAmountWithCurrency(totalAmount, currency)}
              </p>
            </div>
            <button
              type="submit"
              disabled={!canSave}
              className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
            >
              {status === 'submitting' ? 'Сохраняем...' : 'Сохранить прогноз'}
            </button>
          </div>

          {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
          {!isSalaryValid || !isBonusValid ? (
            <p className="text-xs text-rose-600">Разрешены только неотрицательные суммы с 2 знаками.</p>
          ) : null}
        </form>
      )}
    </section>
  )
}

interface IncomeReviewTableProps {
  snapshot: PersonalFinanceSnapshotDto
}

function IncomeReviewTable({ snapshot }: IncomeReviewTableProps) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица доходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          Таблица только для review: факт и прогноз показаны в едином monthly income.
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
              <td className="border-t border-slate-200 px-4 py-3 text-slate-500">По заполненным месяцам</td>
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
  onClick,
}: {
  label: string
  isActive: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-xl px-4 py-2 text-sm font-medium transition ${
        isActive ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'
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

function toMonthLabel(month: number): string {
  return MONTH_LABELS[month - 1] ?? String(month)
}

function defaultActualMonth(year: number): number {
  return year === currentYear() ? currentMonthNumber() : 1
}

function defaultForecastStartMonth(year: number): number {
  if (year !== currentYear()) {
    return 1
  }

  return Math.min(currentMonthNumber() + 1, 12)
}

function resolveForecastStartMonth(year: number, startMonth: number | undefined): number {
  if (startMonth && forecastMonthNumbers(year).includes(startMonth)) {
    return startMonth
  }
  return defaultForecastStartMonth(year)
}

function forecastMonthNumbers(year: number): number[] {
  if (year !== currentYear()) {
    return Array.from({ length: 12 }, (_, index) => index + 1)
  }

  const start = currentMonthNumber() + 1
  if (start > 12) {
    return []
  }

  return Array.from({ length: 12 - start + 1 }, (_, index) => start + index)
}

function currentYear(): number {
  return new Date().getFullYear()
}

function currentMonthNumber(): number {
  return new Date().getMonth() + 1
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
      [category.code]: values[category.code] === '0.00' ? '' : values[category.code],
    }),
    {} as Record<PersonalExpenseCategoryCode, string>,
  )
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
