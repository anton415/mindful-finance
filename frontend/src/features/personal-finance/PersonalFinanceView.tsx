import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import type {
  CreatePersonalFinanceCardRequest,
  PersonalExpenseCategoryCode,
  PersonalExpenseCategoryDto,
  PersonalFinanceCardDto,
  PersonalFinanceIncomePlanDto,
  PersonalFinanceSnapshotDto,
  PersonalFinanceVacationPeriodDto,
  UpdateIncomePlanRequest,
  UpdateMonthlyExpenseRequest,
  UpdateMonthlyIncomeActualRequest,
  UpdatePersonalFinanceCardRequest,
  UpdatePersonalFinanceSettingsRequest,
} from '../../api'

type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
export type PersonalFinanceTab = 'expenses' | 'income' | 'settings'
export interface PersonalFinanceCardListItem extends PersonalFinanceCardDto {
  currentBalance: string | null
  currency: string | null
}

interface AggregatedExpenseMonthViewModel {
  month: number
  actualCategoryAmounts: Record<PersonalExpenseCategoryCode, string>
  limitCategoryAmounts: Record<PersonalExpenseCategoryCode, string>
  actualTotal: string
  limitTotal: string
}

interface AggregatedExpensesViewModel {
  categories: PersonalExpenseCategoryDto[]
  currency: string
  limitCategoryAmounts: Record<PersonalExpenseCategoryCode, string>
  months: AggregatedExpenseMonthViewModel[]
  actualTotalsByCategory: Record<PersonalExpenseCategoryCode, string>
  limitTotalsByCategory: Record<PersonalExpenseCategoryCode, string>
  annualActualTotal: string
  annualLimitTotal: string
  averageMonthlyActualTotal: string
}

export type AggregatedIncomeMonthStatus = 'ACTUAL' | 'FORECAST' | 'OVERRIDE' | 'MIXED' | null

interface AggregatedIncomeMonthViewModel {
  month: number
  totalAmount: string
  status: AggregatedIncomeMonthStatus
}

interface AggregatedIncomeSummaryViewModel {
  totalAmount: string
  activeCardCount: number
  cardsWithForecast: number
}

interface AggregatedIncomeViewModel {
  currency: string
  months: AggregatedIncomeMonthViewModel[]
  annualTotal: string
  averageMonthlyTotal: string
  recurringForecast: AggregatedIncomeSummaryViewModel
}

type IncomeDrawerTab = 'actual' | 'plan'

interface PersonalFinanceViewProps {
  status: LoadStatus
  cards: PersonalFinanceCardListItem[]
  activeSnapshots: PersonalFinanceSnapshotDto[]
  settingsSnapshot: PersonalFinanceSnapshotDto | null
  selectedCardId: string | null
  activeTab: PersonalFinanceTab
  year: number
  errorMessage: string | null
  onSelectTab: (tab: PersonalFinanceTab) => void
  onSelectYear: (year: number) => void
  onSelectCard: (cardId: string) => void
  onRetry: () => void
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveExpenseActual: (
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
  ) => Promise<boolean>
  onSaveIncomeActual: (
    cardId: string,
    month: number,
    request: UpdateMonthlyIncomeActualRequest,
  ) => Promise<boolean>
  onSaveIncomePlan: (
    cardId: string,
    year: number,
    request: UpdateIncomePlanRequest,
  ) => Promise<boolean>
  onRenameCard: (request: UpdatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
  onArchiveCard: (cardId: string) => Promise<boolean>
  onRestoreCard: (cardId: string) => Promise<boolean>
  onDeleteCard: (cardId: string) => Promise<boolean>
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

const DAY_LABELS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'] as const

interface PersonalFinanceTabCopy {
  title: string
  description: string
  actionLabel: string
  panelTitle: string
  panelDescription: string
}

const PERSONAL_FINANCE_TAB_COPY: Record<PersonalFinanceTab, PersonalFinanceTabCopy> = {
  expenses: {
    title: 'Фактические расходы',
    description: 'Годовой обзор суммируется по всем активным картам, а факт сохраняется в выбранную карту.',
    actionLabel: '+ Добавить расходы',
    panelTitle: 'Добавить фактические расходы',
    panelDescription: 'Выберите карту и месяц, чтобы сохранить фактические расходы по категориям.',
  },
  income: {
    title: 'Доходы по месяцам',
    description:
      'Годовой обзор суммируется по всем активным картам: факт имеет приоритет, а planner отпусков и 13-й зарплаты производит derived overrides поверх recurring forecast.',
    actionLabel: '+ Факт и план',
    panelTitle: 'Факт и план доходов',
    panelDescription:
      'Сохраняйте фактический доход по месяцу или настраивайте годовой planner отпусков и 13-й зарплаты для derived income overrides.',
  },
  settings: {
    title: 'Настройки карты',
    description: 'Настройки остаются в основном контенте, а новую карту можно добавить через панель справа.',
    actionLabel: '+ Добавить карту',
    panelTitle: 'Добавить новую карту',
    panelDescription: 'Создайте карту с внутренним cash-ledger для доходов, расходов и текущего баланса.',
  },
}

export function PersonalFinanceView({
  status,
  cards,
  activeSnapshots,
  settingsSnapshot,
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
  onSaveIncomePlan,
  onRenameCard,
  onSaveSettings,
  onArchiveCard,
  onRestoreCard,
  onDeleteCard,
}: PersonalFinanceViewProps) {
  const [isActionPanelOpen, setIsActionPanelOpen] = useState<boolean>(false)

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

  const activeCards = cards.filter((card) => card.status === 'ACTIVE')
  const archivedCards = cards.filter((card) => card.status === 'ARCHIVED')
  const hasAnyCards = cards.length > 0
  const hasActiveCards = activeCards.length > 0
  const hasLoadedActiveSnapshots = activeSnapshots.length > 0
  const selectedCard = settingsSnapshot?.card ?? cards.find((card) => card.id === selectedCardId) ?? null
  const activeTabCopy = PERSONAL_FINANCE_TAB_COPY[activeTab]
  const yearOptions = selectableYearOptions(year)
  const canOpenActionPanel = activeTab === 'settings' || hasLoadedActiveSnapshots
  const aggregatedExpenses = hasLoadedActiveSnapshots ? aggregateExpenses(activeSnapshots) : null
  const aggregatedIncome = hasLoadedActiveSnapshots ? aggregateIncome(activeSnapshots) : null

  const handleTabSelect = (tab: PersonalFinanceTab): void => {
    setIsActionPanelOpen(false)
    onSelectTab(tab)
  }

  const handleYearSelect = (nextYear: number): void => {
    setIsActionPanelOpen(false)
    onSelectYear(nextYear)
  }

  const handleCardSelect = (cardId: string): void => {
    setIsActionPanelOpen(false)
    onSelectCard(cardId)
  }

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-slate-50/70 p-4 lg:p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">Личные финансы</h2>
            <p className="mt-1 max-w-3xl text-sm text-slate-600">
              У каждой карты есть внутренний cash-ledger: факт расходов и доходов меняет баланс,
              а лимиты, цели и прогноз живут в настройках. Большинство расходных категорий задаются на месяц,
              развлечения и обучение задаются на год, а инвестиции идут отдельной годовой целью перевода.
            </p>
          </div>

          <label className="w-full text-sm text-slate-600 sm:max-w-52">
            Год
            <select
              value={year}
              onChange={(event) => handleYearSelect(Number(event.target.value))}
              disabled={!hasAnyCards}
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

        {activeTab === 'settings' ? (
          <PersonalFinanceCardSelector
            cards={cards}
            selectedCardId={selectedCard?.id ?? null}
            onSelectCard={handleCardSelect}
          />
        ) : null}
      </section>

      <section className="rounded-3xl border border-slate-200 bg-white p-4 lg:p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <nav className="inline-flex rounded-2xl border border-slate-200 bg-slate-50 p-1">
            <NestedTabButton
              label="Расходы"
              isActive={activeTab === 'expenses'}
              onClick={() => handleTabSelect('expenses')}
            />
            <NestedTabButton
              label="Доходы"
              isActive={activeTab === 'income'}
              onClick={() => handleTabSelect('income')}
            />
            <NestedTabButton
              label="Настройки"
              isActive={activeTab === 'settings'}
              onClick={() => handleTabSelect('settings')}
            />
          </nav>

          <button
            type="button"
            onClick={() => setIsActionPanelOpen(true)}
            disabled={!canOpenActionPanel}
            className="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {activeTabCopy.actionLabel}
          </button>
        </div>

        <div className="mt-4 border-t border-slate-200 pt-4">
          <h3 className="text-base font-semibold text-slate-900">{activeTabCopy.title}</h3>
          <p className="mt-1 text-sm text-slate-600">{activeTabCopy.description}</p>
        </div>
      </section>

      {activeTab === 'settings' && settingsSnapshot?.card.status === 'ARCHIVED' ? (
        <ArchivedCardBanner cardName={settingsSnapshot.card.name} />
      ) : null}

      {activeTab === 'expenses' ? (
        aggregatedExpenses ? (
          <ExpensesTab key={`expenses-${year}-${activeCards.length}`} aggregate={aggregatedExpenses} />
        ) : hasActiveCards ? (
          <InlineStatus
            tone="warning"
            message="Не удалось полностью загрузить данные активных карт. Откройте настройки нужной карты или повторите попытку."
            actionLabel="Повторить"
            onAction={onRetry}
          />
        ) : (
          <PersonalFinanceAggregateEmptyState
            tabLabel="расходов"
            hasArchivedCards={archivedCards.length > 0}
          />
        )
      ) : activeTab === 'income' ? (
        aggregatedIncome ? (
          <IncomeTab key={`income-${year}-${activeCards.length}`} aggregate={aggregatedIncome} />
        ) : hasActiveCards ? (
          <InlineStatus
            tone="warning"
            message="Не удалось полностью загрузить данные активных карт. Откройте настройки нужной карты или повторите попытку."
            actionLabel="Повторить"
            onAction={onRetry}
          />
        ) : (
          <PersonalFinanceAggregateEmptyState
            tabLabel="доходов"
            hasArchivedCards={archivedCards.length > 0}
          />
        )
      ) : !hasAnyCards ? (
        <PersonalFinanceSettingsEmptyState hasArchivedCards={archivedCards.length > 0} />
      ) : !settingsSnapshot ? (
        <InlineStatus tone="neutral" message="Выберите карту, чтобы открыть настройки." />
      ) : (
        <SettingsTab
          key={`settings-${settingsSnapshot.card.id}-${settingsSnapshot.card.name}-${settingsSnapshot.year}-${settingsSnapshot.settings.currentBalance}`}
          snapshot={settingsSnapshot}
          onRenameCard={onRenameCard}
          onSaveSettings={onSaveSettings}
          onArchiveCard={onArchiveCard}
          onRestoreCard={onRestoreCard}
          onDeleteCard={onDeleteCard}
        />
      )}

      {isActionPanelOpen ? (
        <DrawerShell
          title={activeTabCopy.panelTitle}
          description={activeTabCopy.panelDescription}
          onClose={() => setIsActionPanelOpen(false)}
        >
          {activeTab === 'expenses' && hasLoadedActiveSnapshots ? (
            <ExpenseEntryDrawerPanel
              activeSnapshots={activeSnapshots}
              preferredCardId={selectedCardId}
              onSaveExpenseActual={onSaveExpenseActual}
              onClose={() => setIsActionPanelOpen(false)}
            />
          ) : null}
          {activeTab === 'income' && hasLoadedActiveSnapshots ? (
            <IncomeEntryDrawerPanel
              activeSnapshots={activeSnapshots}
              preferredCardId={selectedCardId}
              onSaveIncomeActual={onSaveIncomeActual}
              onSaveIncomePlan={onSaveIncomePlan}
              onClose={() => setIsActionPanelOpen(false)}
            />
          ) : null}
          {activeTab === 'settings' ? (
            <CreateCardDrawerPanel
              onCreateCard={onCreateCard}
              onSuccess={() => setIsActionPanelOpen(false)}
            />
          ) : null}
        </DrawerShell>
      ) : null}
    </div>
  )
}

interface ExpensesTabProps {
  aggregate: AggregatedExpensesViewModel
}

function ExpensesTab({ aggregate }: ExpensesTabProps) {
  return <ExpenseReviewTable aggregate={aggregate} />
}

interface ExpenseEntryDrawerPanelProps {
  activeSnapshots: PersonalFinanceSnapshotDto[]
  preferredCardId: string | null
  onSaveExpenseActual: (
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
  ) => Promise<boolean>
  onClose: () => void
}

function ExpenseEntryDrawerPanel({
  activeSnapshots,
  preferredCardId,
  onSaveExpenseActual,
  onClose,
}: ExpenseEntryDrawerPanelProps) {
  const defaultCardId = resolveDefaultActiveCardId(activeSnapshots, preferredCardId)
  const [selectedCardId, setSelectedCardId] = useState<string>(defaultCardId)
  const selectedSnapshot =
    activeSnapshots.find((snapshot) => snapshot.card.id === selectedCardId) ?? activeSnapshots[0]
  const [selectedMonth, setSelectedMonth] = useState<number>(() => defaultActualMonth(selectedSnapshot.year))
  const selectedMonthData =
    selectedSnapshot.expenses.months.find((month) => month.month === selectedMonth) ??
    selectedSnapshot.expenses.months[0]

  return (
    <ExpenseEntryFormCard
      key={`actual-${selectedSnapshot.card.id}-${selectedSnapshot.year}-${selectedMonth}-${serializeExpenseMonth(selectedMonthData.actualCategoryAmounts, selectedSnapshot.categories)}`}
      title="Расходы по категориям"
      description="Лимиты и цели задаются на вкладке настроек: большинство расходов помесячно, развлечения и обучение на год, инвестиции как годовая цель перевода."
      submitLabel="Сохранить факт"
      cards={activeSnapshots.map((snapshot) => snapshot.card)}
      selectedCardId={selectedSnapshot.card.id}
      onSelectCard={setSelectedCardId}
      selectedMonth={selectedMonth}
      onSelectMonth={setSelectedMonth}
      year={selectedSnapshot.year}
      currency={selectedSnapshot.currency}
      categories={selectedSnapshot.categories}
      initialValues={selectedMonthData.actualCategoryAmounts}
      onSave={onSaveExpenseActual}
      onSaved={onClose}
    />
  )
}

interface IncomeTabProps {
  aggregate: AggregatedIncomeViewModel
}

function IncomeTab({ aggregate }: IncomeTabProps) {
  return (
    <div className="space-y-4">
      <RecurringIncomeSummaryCard currency={aggregate.currency} summary={aggregate.recurringForecast} />

      <IncomeReviewTable aggregate={aggregate} />
    </div>
  )
}

interface IncomeEntryDrawerPanelProps {
  activeSnapshots: PersonalFinanceSnapshotDto[]
  preferredCardId: string | null
  onSaveIncomeActual: (
    cardId: string,
    month: number,
    request: UpdateMonthlyIncomeActualRequest,
  ) => Promise<boolean>
  onSaveIncomePlan: (
    cardId: string,
    year: number,
    request: UpdateIncomePlanRequest,
  ) => Promise<boolean>
  onClose: () => void
}

function IncomeEntryDrawerPanel({
  activeSnapshots,
  preferredCardId,
  onSaveIncomeActual,
  onSaveIncomePlan,
  onClose,
}: IncomeEntryDrawerPanelProps) {
  const defaultCardId = resolveDefaultActiveCardId(activeSnapshots, preferredCardId)
  const [selectedCardId, setSelectedCardId] = useState<string>(defaultCardId)
  const [selectedDrawerTab, setSelectedDrawerTab] = useState<IncomeDrawerTab>('actual')
  const selectedSnapshot =
    activeSnapshots.find((snapshot) => snapshot.card.id === selectedCardId) ?? activeSnapshots[0]
  const [selectedMonth, setSelectedMonth] = useState<number>(() => defaultActualMonth(selectedSnapshot.year))

  const selectedMonthData =
    selectedSnapshot.income.months.find((month) => month.month === selectedMonth) ??
    selectedSnapshot.income.months[0]
  const derivedPlanPreview = deriveIncomePlanPreview(
    selectedSnapshot.incomePlan,
    selectedSnapshot.settings.incomeForecast?.salaryAmount ?? '0.00',
  )

  return (
    <div className="space-y-4">
      <section className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h3 className="text-base font-semibold text-slate-900">Карта и режим ввода</h3>
            <p className="mt-1 text-sm text-slate-600">
              Факт двигает monthly review и linked balance. Planner отпусков и 13-й зарплаты меняет только planning-слой года.
            </p>
          </div>

          <div className="flex w-full flex-col gap-3 lg:max-w-4xl lg:flex-row lg:items-end">
            <div className="w-full lg:max-w-sm">
              <p className="mb-1 text-sm text-slate-600">Режим</p>
              <div className="inline-flex rounded-2xl border border-slate-200 bg-white p-1">
                <NestedTabButton
                  label="Факт"
                  isActive={selectedDrawerTab === 'actual'}
                  onClick={() => setSelectedDrawerTab('actual')}
                />
                <NestedTabButton
                  label="План доходов"
                  isActive={selectedDrawerTab === 'plan'}
                  onClick={() => setSelectedDrawerTab('plan')}
                />
              </div>
            </div>

            <CardField
              cards={activeSnapshots.map((snapshot) => snapshot.card)}
              selectedCardId={selectedSnapshot.card.id}
              onSelectCard={setSelectedCardId}
            />

            {selectedDrawerTab === 'actual' ? (
              <label className="w-full text-sm text-slate-600 sm:max-w-64">
                Месяц
                <select
                  value={selectedMonth}
                  onChange={(event) => setSelectedMonth(Number(event.target.value))}
                  className={selectClassName()}
                >
                  {MONTH_LABELS.map((label, index) => (
                    <option key={label} value={index + 1}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <div className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 sm:max-w-64">
                <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Год planner</p>
                <p className="mt-1 text-sm font-semibold text-slate-900">{selectedSnapshot.year}</p>
                <p className="mt-1 text-xs text-slate-500">План сохраняется целиком на выбранный год.</p>
              </div>
            )}
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <MetricTile
            label="Recurring forecast"
            value={formatAmountWithCurrency(
              selectedSnapshot.settings.incomeForecast?.totalAmount ?? '0.00',
              selectedSnapshot.currency,
            )}
            hint="Это базовый monthly forecast без учёта planner-derived adjustments."
          />
          {selectedDrawerTab === 'actual' ? (
            <>
              <MetricTile
                label="Статус в review"
                value={selectedMonthData.status ? incomeStatusLabel(selectedMonthData.status) : 'Нет данных'}
                hint="Факт имеет приоритет. Override подмешивается только при отсутствии факта."
              />
              <MetricTile
                label="Сумма в review"
                value={
                  selectedMonthData.status
                    ? formatAmountWithCurrency(selectedMonthData.totalAmount, selectedSnapshot.currency)
                    : '—'
                }
                hint={`Сейчас выбран ${toMonthLabel(selectedMonth)}.`}
              />
            </>
          ) : (
            <>
              <MetricTile
                label="13-я зарплата"
                value={
                  derivedPlanPreview.thirteenthSalaryMonth
                    ? toMonthLabel(derivedPlanPreview.thirteenthSalaryMonth)
                    : 'Не запланирована'
                }
                hint="Если включена, planner добавляет +оклад в выбранный месяц."
              />
              <MetricTile
                label="Основные отпускные"
                value={
                  derivedPlanPreview.vacationPayoutMonth
                    ? toMonthLabel(derivedPlanPreview.vacationPayoutMonth)
                    : 'Нет отпуска >= 14 дней'
                }
                hint="Берётся первый merged-период отпуска длиной от 14 дней включительно."
              />
            </>
          )}
        </div>
      </section>

      {selectedDrawerTab === 'actual' ? (
        <IncomeActualFormCard
          key={`actual-income-${selectedSnapshot.card.id}-${selectedSnapshot.year}-${selectedMonth}-${selectedMonthData.totalAmount}-${selectedMonthData.status ?? 'EMPTY'}`}
          cardId={selectedSnapshot.card.id}
          year={selectedSnapshot.year}
          currency={selectedSnapshot.currency}
          selectedMonth={selectedMonth}
          initialAmount={selectedMonthData.status === 'ACTUAL' ? selectedMonthData.totalAmount : ''}
          onSave={onSaveIncomeActual}
          onSaved={onClose}
        />
      ) : (
        <IncomePlanFormCard
          key={`plan-income-${selectedSnapshot.card.id}-${selectedSnapshot.year}-${serializeIncomePlan(selectedSnapshot.incomePlan)}`}
          cardId={selectedSnapshot.card.id}
          year={selectedSnapshot.year}
          currency={selectedSnapshot.currency}
          salaryAmount={selectedSnapshot.settings.incomeForecast?.salaryAmount ?? '0.00'}
          baseForecastAmount={selectedSnapshot.settings.incomeForecast?.totalAmount ?? '0.00'}
          initialIncomePlan={selectedSnapshot.incomePlan}
          incomeMonths={selectedSnapshot.income.months}
          onSave={onSaveIncomePlan}
          onSaved={onClose}
        />
      )}
    </div>
  )
}

interface SettingsTabProps {
  snapshot: PersonalFinanceSnapshotDto
  onRenameCard: (request: UpdatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
  onArchiveCard: (cardId: string) => Promise<boolean>
  onRestoreCard: (cardId: string) => Promise<boolean>
  onDeleteCard: (cardId: string) => Promise<boolean>
}

function SettingsTab({
  snapshot,
  onRenameCard,
  onSaveSettings,
  onArchiveCard,
  onRestoreCard,
  onDeleteCard,
}: SettingsTabProps) {
  return (
    <SettingsDetails
      snapshot={snapshot}
      onRenameCard={onRenameCard}
      onSaveSettings={onSaveSettings}
      onArchiveCard={onArchiveCard}
      onRestoreCard={onRestoreCard}
      onDeleteCard={onDeleteCard}
    />
  )
}

function PersonalFinanceAggregateEmptyState({
  tabLabel,
  hasArchivedCards,
}: {
  tabLabel: string
  hasArchivedCards: boolean
}) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5">
      <h3 className="text-base font-semibold text-slate-900">Нет активных карт для {tabLabel}</h3>
      <p className="mt-2 text-sm text-slate-600">
        Годовой обзор доходов и расходов строится только по активному personal finance контуру.
      </p>
      <p className="mt-3 text-sm text-slate-500">
        {hasArchivedCards
          ? 'Откройте вкладку настроек, чтобы вернуть карту из архива, или создайте новую карту.'
          : 'Откройте вкладку настроек и добавьте первую карту, чтобы начать yearly review.'}
      </p>
    </section>
  )
}

function SettingsDetails({
  snapshot,
  onRenameCard,
  onSaveSettings,
  onArchiveCard,
  onRestoreCard,
  onDeleteCard,
}: {
  snapshot: PersonalFinanceSnapshotDto
  onRenameCard: (request: UpdatePersonalFinanceCardRequest) => Promise<boolean>
  onSaveSettings: (request: UpdatePersonalFinanceSettingsRequest) => Promise<boolean>
  onArchiveCard: (cardId: string) => Promise<boolean>
  onRestoreCard: (cardId: string) => Promise<boolean>
  onDeleteCard: (cardId: string) => Promise<boolean>
}) {
  const isArchived = snapshot.card.status === 'ARCHIVED'
  const [cardName, setCardName] = useState<string>(snapshot.card.name)
  const [baselineAmount, setBaselineAmount] = useState<string>(
    toDraftAmount(snapshot.settings.baselineAmount),
  )
  const [salaryAmount, setSalaryAmount] = useState<string>(
    toDraftAmount(snapshot.settings.incomeForecast?.salaryAmount ?? '0.00'),
  )
  const [bonusPercent, setBonusPercent] = useState<string>(
    toDraftAmount(snapshot.settings.incomeForecast?.bonusPercent ?? '0.00'),
  )
  const [limitPercentValues, setLimitPercentValues] = useState<Record<PersonalExpenseCategoryCode, string>>(() =>
    toExpenseDraftValues(snapshot.settings.limitCategoryPercents, snapshot.categories),
  )
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [renameStatus, setRenameStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [renameErrorMessage, setRenameErrorMessage] = useState<string | null>(null)
  const [cardActionStatus, setCardActionStatus] = useState<'idle' | 'archiving' | 'restoring' | 'deleting' | 'error'>('idle')
  const [cardActionErrorMessage, setCardActionErrorMessage] = useState<string | null>(null)

  const areBaseValuesValid =
    isValidNonNegativeAmountValue(baselineAmount) &&
    isValidNonNegativeAmountValue(salaryAmount) &&
    isValidNonNegativeAmountValue(bonusPercent)
  const areLimitsValid = snapshot.categories.every((category) =>
    isValidNonNegativeAmountValue(limitPercentValues[category.code]),
  )
  const canSave = !isArchived && areBaseValuesValid && areLimitsValid && status !== 'submitting'
  const monthlyForecast = calculateForecastAmount(salaryAmount, bonusPercent)
  const configuredLimitTotals = calculateConfiguredLimitTotals(
    snapshot.categories,
    limitPercentValues,
    monthlyForecast,
  )
  const isForecastMissing = compareDecimalStrings(monthlyForecast, '0.00') === 0
  const isCardNameValid = cardName.trim().length > 0
  const canSaveCardName =
    !isArchived && isCardNameValid && cardName.trim() !== snapshot.card.name && renameStatus !== 'submitting'
  const isCardActionBusy = cardActionStatus !== 'idle' && cardActionStatus !== 'error'

  const handleRenameSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSaveCardName) {
      return
    }

    setRenameStatus('submitting')
    setRenameErrorMessage(null)

    const renamed = await onRenameCard({ name: cardName.trim() })
    if (renamed) {
      setRenameStatus('idle')
      return
    }

    setRenameStatus('error')
    setRenameErrorMessage('Не удалось сохранить название карты.')
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSave) {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)

    const saved = await onSaveSettings({
      baselineAmount: toDecimalAmountString(baselineAmount),
      limitCategoryPercents: snapshot.categories.reduce(
        (result, category) => ({
          ...result,
          [category.code]: toDecimalAmountString(limitPercentValues[category.code]),
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

  const handleArchiveClick = async (): Promise<void> => {
    if (
      !window.confirm(
        `Архивировать карту «${snapshot.card.name}»? Она исчезнет из активного контура, а linked account перестанет влиять на текущие метрики.`,
      )
    ) {
      return
    }

    setCardActionStatus('archiving')
    setCardActionErrorMessage(null)

    const archived = await onArchiveCard(snapshot.card.id)
    if (!archived) {
      setCardActionStatus('error')
      setCardActionErrorMessage('Не удалось архивировать карту.')
      return
    }

    setCardActionStatus('idle')
  }

  const handleRestoreClick = async (): Promise<void> => {
    setCardActionStatus('restoring')
    setCardActionErrorMessage(null)

    const restored = await onRestoreCard(snapshot.card.id)
    if (!restored) {
      setCardActionStatus('error')
      setCardActionErrorMessage('Не удалось вернуть карту из архива.')
      return
    }

    setCardActionStatus('idle')
  }

  const handleDeleteClick = async (): Promise<void> => {
    if (
      !window.confirm(
        `Удалить карту «${snapshot.card.name}» навсегда? Это удалит связанные доходы, расходы, настройки и linked cash account без возможности восстановления.`,
      )
    ) {
      return
    }

    setCardActionStatus('deleting')
    setCardActionErrorMessage(null)

    const deleted = await onDeleteCard(snapshot.card.id)
    if (!deleted) {
      setCardActionStatus('error')
      setCardActionErrorMessage('Не удалось удалить карту.')
      return
    }

    setCardActionStatus('idle')
  }

  return (
    <div className="space-y-4">
      <form
        className="rounded-3xl border border-slate-200 bg-white p-4"
        onSubmit={(event) => {
          void handleRenameSubmit(event)
        }}
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0 flex-1">
            <h3 className="text-base font-semibold text-slate-900">Название карты</h3>
            <p className="mt-1 text-sm text-slate-600">
              Это имя используется и для внутреннего cash-ledger карты.
            </p>
            <label className="mt-4 block text-sm text-slate-600">
              Как карта называется в обзоре
              <input
                type="text"
                value={cardName}
                disabled={isArchived}
                onChange={(event) => {
                  setCardName(event.target.value)
                  setRenameStatus('idle')
                  setRenameErrorMessage(null)
                }}
                placeholder="Например, T-Банк Black"
                className={inputClassName(isCardNameValid)}
              />
            </label>
          </div>

          <button
            type="submit"
            disabled={!canSaveCardName}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {renameStatus === 'submitting' ? 'Сохраняем...' : 'Сохранить название'}
          </button>
        </div>

        {renameErrorMessage ? <p className="mt-3 text-xs text-rose-600">{renameErrorMessage}</p> : null}
        {isArchived ? (
          <p className="mt-3 text-xs text-slate-500">Архивная карта доступна только для просмотра.</p>
        ) : null}
        {!isCardNameValid ? (
          <p className="mt-3 text-xs text-rose-600">Название карты не может быть пустым.</p>
        ) : null}
      </form>

      <section className="grid gap-4 lg:grid-cols-3">
        <MetricTile
          label="Текущий баланс карты"
          value={formatAmountWithCurrency(snapshot.settings.currentBalance, snapshot.currency)}
          hint="Считается из baseline и фактических доходов/расходов."
        />
        <MetricTile
          label="Месячные лимиты расходов"
          value={formatAmountWithCurrency(configuredLimitTotals.monthlyLimitTotal, snapshot.currency)}
          hint="Сумма категорий, которые сравниваются по каждому месяцу."
        />
        <MetricTile
          label="Годовой лимит расходов"
          value={formatAmountWithCurrency(configuredLimitTotals.annualLimitTotal, snapshot.currency)}
          hint="Полный бюджет расходов на год: месячные категории умножаются на 12, годовые расходные категории добавляются сверху."
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
            Проценты задаются от recurring monthly forecast: расходные категории сравниваются
            по месяцу или по году, а инвестиции используются как годовая цель перевода в инвестиционные счета.
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
                disabled={isArchived}
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
              <h4 className="text-sm font-semibold text-slate-900">Лимиты и цели по категориям</h4>
              <p className="mt-1 text-sm text-slate-600">
                Вводите проценты, а денежные лимиты и цели пересчитываются автоматически от прогноза.
              </p>
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
                <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Месячные лимиты расходов</p>
                <p className="mt-1 text-sm font-semibold text-slate-900">
                  {formatAmountWithCurrency(configuredLimitTotals.monthlyLimitTotal, snapshot.currency)}
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
                <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Годовой лимит расходов</p>
                <p className="mt-1 text-sm font-semibold text-slate-900">
                  {formatAmountWithCurrency(configuredLimitTotals.annualLimitTotal, snapshot.currency)}
                </p>
              </div>
            </div>
          </div>

          {isForecastMissing ? (
            <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
              Прогноз в месяц равен 0.00. Проценты можно сохранить уже сейчас, но денежные лимиты
              останутся нулевыми, пока не заполнен прогноз доходов.
            </p>
          ) : null}

          <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {snapshot.categories.map((category) => (
              <label key={category.code} className="text-sm text-slate-600">
                {category.label} ({limitPercentInputLabel(category)})
                <input
                  type="text"
                  inputMode="decimal"
                  value={limitPercentValues[category.code]}
                  disabled={isArchived}
                  onChange={(event) =>
                    setLimitPercentValues((current) => ({
                      ...current,
                      [category.code]: normalizeAmountInput(event.target.value),
                    }))
                  }
                  placeholder="0.00"
                  className={inputClassName(isValidNonNegativeAmountValue(limitPercentValues[category.code]))}
                />
                <span className="mt-1 block text-xs text-slate-500">
                  {categoryAmountLabel(category)}:{' '}
                  {formatAmountWithCurrency(
                    calculateConfiguredLimitAmount(category, limitPercentValues[category.code], monthlyForecast),
                    snapshot.currency,
                  )}
                </span>
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
                disabled={isArchived}
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
                disabled={isArchived}
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
            {isArchived
              ? 'Архивная карта зафиксирована: сначала верните её в активные, если нужно снова редактировать данные.'
              : 'Только фактические доходы и расходы двигают баланс linked account.'}
          </div>
          {!isArchived ? (
            <button
              type="submit"
              disabled={!canSave}
              className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
            >
              {status === 'submitting' ? 'Сохраняем...' : 'Сохранить настройки'}
            </button>
          ) : null}
        </div>

        {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
        {!areBaseValuesValid || !areLimitsValid ? (
          <p className="text-xs text-rose-600">Разрешены только неотрицательные значения с 2 знаками.</p>
        ) : null}
      </form>

      {isArchived ? (
        <section className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
          <h3 className="text-base font-semibold text-slate-900">Карта в архиве</h3>
          <p className="mt-1 text-sm text-slate-600">
            Данные сохранены, но карта и linked account исключены из активного личного контура и текущих метрик.
          </p>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => {
                void handleRestoreClick()
              }}
              disabled={isCardActionBusy}
              className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
            >
              {cardActionStatus === 'restoring' ? 'Возвращаем...' : 'Вернуть из архива'}
            </button>
            <button
              type="button"
              onClick={() => {
                void handleDeleteClick()
              }}
              disabled={isCardActionBusy}
              className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-sm font-semibold text-rose-700 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
            >
              {cardActionStatus === 'deleting' ? 'Удаляем...' : 'Удалить навсегда'}
            </button>
          </div>
          {cardActionErrorMessage ? <p className="mt-3 text-xs text-rose-600">{cardActionErrorMessage}</p> : null}
        </section>
      ) : (
        <section className="rounded-3xl border border-rose-200 bg-rose-50 p-4">
          <h3 className="text-base font-semibold text-rose-700">Управление картой</h3>
          <p className="mt-1 text-sm text-rose-700/80">
            Архивируйте карту, если хотите убрать её из активного контура без потери истории. Удаление навсегда
            вычищает все связанные данные.
          </p>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => {
                void handleArchiveClick()
              }}
              disabled={isCardActionBusy}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-900 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
            >
              {cardActionStatus === 'archiving' ? 'Архивируем...' : 'Архивировать'}
            </button>
            <button
              type="button"
              onClick={() => {
                void handleDeleteClick()
              }}
              disabled={isCardActionBusy}
              className="rounded-xl border border-rose-200 bg-rose-100 px-4 py-2.5 text-sm font-semibold text-rose-700 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
            >
              {cardActionStatus === 'deleting' ? 'Удаляем...' : 'Удалить навсегда'}
            </button>
          </div>
          {cardActionErrorMessage ? <p className="mt-3 text-xs text-rose-600">{cardActionErrorMessage}</p> : null}
        </section>
      )}
    </div>
  )
}

function PersonalFinanceSettingsEmptyState({ hasArchivedCards }: { hasArchivedCards: boolean }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5">
      <h3 className="text-base font-semibold text-slate-900">Добавьте первую карту</h3>
      <p className="mt-2 text-sm text-slate-600">
        Используйте кнопку + Добавить карту вверху справа на вкладке настроек, чтобы создать
        personal finance card. После этого появятся расходы, доходы и годовой обзор.
      </p>
      {hasArchivedCards ? (
        <p className="mt-3 text-sm text-slate-500">
          Ниже в секции архива можно открыть старую карту, вернуть её в активные или удалить навсегда.
        </p>
      ) : null}
    </section>
  )
}

function CreateCardDrawerPanel({
  onCreateCard,
  onSuccess,
}: {
  onCreateCard: (request: CreatePersonalFinanceCardRequest) => Promise<boolean>
  onSuccess: () => void
}) {
  const [newCardName, setNewCardName] = useState<string>('')
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (newCardName.trim().length === 0 || status === 'submitting') {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)

    const created = await onCreateCard({ name: newCardName.trim() })
    if (created) {
      setNewCardName('')
      setStatus('idle')
      onSuccess()
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось добавить карту.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <div>
        <h3 className="text-base font-semibold text-slate-900">Новая карта</h3>
        <p className="mt-1 text-sm text-slate-600">
          У каждой карты будет свой внутренний cash-ledger для расходов, доходов и текущего баланса.
        </p>
      </div>

      <form
        className="mt-4 space-y-3"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <label className="text-sm text-slate-600">
          Название карты
          <input
            type="text"
            value={newCardName}
            onChange={(event) => setNewCardName(event.target.value)}
            placeholder="Например, T-Банк Black"
            className="mt-1 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none"
          />
        </label>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">После создания карта сразу появится в списке на вкладке настроек.</p>
          <button
            type="submit"
            disabled={newCardName.trim().length === 0 || status === 'submitting'}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {status === 'submitting' ? 'Добавляем...' : 'Добавить карту'}
          </button>
        </div>

        {errorMessage ? <p className="text-xs text-rose-600">{errorMessage}</p> : null}
      </form>
    </section>
  )
}

function RecurringIncomeSummaryCard({
  currency,
  summary,
}: {
  currency: string
  summary: AggregatedIncomeSummaryViewModel
}) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <h3 className="text-base font-semibold text-slate-900">Повторяющийся прогноз доходов</h3>
      <p className="mt-1 text-sm text-slate-600">
        Здесь показан только базовый recurring forecast по активным картам. Planner-derived overrides видны в таблице по месяцам ниже.
      </p>
      {isPositiveAmount(summary.totalAmount) ? (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <MetricTile
            label="Прогноз в месяц"
            value={formatAmountWithCurrency(summary.totalAmount, currency)}
            hint="Сумма base recurring forecast по всем активным картам."
          />
          <MetricTile
            label="Карты с прогнозом"
            value={`${summary.cardsWithForecast} из ${summary.activeCardCount}`}
            hint="Карты без base recurring forecast в сумме не участвуют."
          />
        </div>
      ) : (
        <p className="mt-2 text-sm text-slate-600">
          Повторяющийся прогноз ещё не задан ни для одной активной карты. Его можно настроить на вкладке настроек.
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
  const activeCards = cards.filter((card) => card.status === 'ACTIVE')
  const archivedCards = cards.filter((card) => card.status === 'ARCHIVED')

  return (
    <article className="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Карты личных финансов</h3>
          <p className="mt-1 text-xs text-slate-500">
            Выберите карту, чтобы открыть её настройки, архив или linked balance.
          </p>
        </div>
      </div>

      {cards.length === 0 ? (
        <div className="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
          Пока нет карт. Создание доступно через кнопку на вкладке настроек.
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          <CardListSection
            title="Активные карты"
            emptyMessage="Активных карт сейчас нет."
            cards={activeCards}
            selectedCardId={selectedCardId}
            onSelectCard={onSelectCard}
          />
          {archivedCards.length > 0 ? (
            <CardListSection
              title="Архив"
              cards={archivedCards}
              selectedCardId={selectedCardId}
              onSelectCard={onSelectCard}
              tone="archived"
            />
          ) : null}
        </div>
      )}
    </article>
  )
}

function CardListSection({
  title,
  cards,
  selectedCardId,
  onSelectCard,
  emptyMessage,
  tone = 'active',
}: {
  title: string
  cards: PersonalFinanceCardListItem[]
  selectedCardId: string | null
  onSelectCard: (cardId: string) => void
  emptyMessage?: string
  tone?: 'active' | 'archived'
}) {
  return (
    <section>
      <div className="flex items-center justify-between gap-3">
        <h4 className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{title}</h4>
        <span className="text-xs text-slate-400">{cards.length}</span>
      </div>

      {cards.length === 0 ? (
        <div className="mt-3 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-500">
          {emptyMessage ?? 'Нет карточек в этом разделе.'}
        </div>
      ) : (
        <ul className="mt-3 space-y-2">
          {cards.map((card) => (
            <li key={card.id}>
              <button
                type="button"
                onClick={() => onSelectCard(card.id)}
                className={`w-full rounded-lg border px-3 py-3 text-left transition ${
                  selectedCardId === card.id
                    ? tone === 'archived'
                      ? 'border-slate-300 bg-slate-100 shadow-sm'
                      : 'border-slate-300 bg-slate-50 shadow-sm'
                    : tone === 'archived'
                      ? 'border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-slate-100'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/70'
                }`}
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-slate-900">{card.name}</p>
                      {tone === 'archived' ? (
                        <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-600">
                          Архив
                        </span>
                      ) : null}
                    </div>
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
    </section>
  )
}

function ArchivedCardBanner({ cardName }: { cardName: string }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-slate-100 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Архив</p>
      <h3 className="mt-2 text-base font-semibold text-slate-900">{cardName}</h3>
      <p className="mt-1 text-sm text-slate-600">
        Карта открыта в режиме просмотра. Доходы, расходы и настройки заморожены, пока вы не вернёте её в активные.
      </p>
    </section>
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

function CardField({
  cards,
  selectedCardId,
  onSelectCard,
}: {
  cards: PersonalFinanceCardDto[]
  selectedCardId: string
  onSelectCard: (cardId: string) => void
}) {
  return (
    <label className="w-full text-sm text-slate-600 sm:max-w-72">
      Карта
      <select
        value={selectedCardId}
        onChange={(event) => onSelectCard(event.target.value)}
        className={selectClassName()}
      >
        {cards.map((card) => (
          <option key={card.id} value={card.id}>
            {card.name}
          </option>
        ))}
      </select>
    </label>
  )
}

interface ExpenseEntryFormCardProps {
  title: string
  description: string
  submitLabel: string
  cards: PersonalFinanceCardDto[]
  selectedCardId: string
  onSelectCard: (cardId: string) => void
  selectedMonth: number
  onSelectMonth: (month: number) => void
  year: number
  currency: string
  categories: PersonalExpenseCategoryDto[]
  initialValues: Record<PersonalExpenseCategoryCode, string>
  onSave: (
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
  ) => Promise<boolean>
  onSaved?: () => void
}

function ExpenseEntryFormCard({
  title,
  description,
  submitLabel,
  cards,
  selectedCardId,
  onSelectCard,
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

    const saved = await onSave(selectedCardId, selectedMonth, {
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
        <div className="flex w-full flex-col gap-3 lg:max-w-xl lg:flex-row">
          <CardField cards={cards} selectedCardId={selectedCardId} onSelectCard={onSelectCard} />
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

function ExpenseReviewTable({ aggregate }: { aggregate: AggregatedExpensesViewModel }) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица расходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          Факт и totals считаются только по расходным категориям. Инвестиции остаются в таблице отдельной серой
          колонкой как перевод на инвестиционные счета и не входят в общий расход.
        </p>
      </div>

      <div className="lg:hidden">
        <div className="space-y-3 p-4">
          {aggregate.months.map((month) => (
            <article key={month.month} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h4 className="text-sm font-semibold text-slate-900">{toMonthLabel(month.month)}</h4>
                  <p className="mt-1 text-xs text-slate-500">
                    Факт {formatAmount(month.actualTotal)} · Месячный лимит {formatAmount(month.limitTotal)}
                  </p>
                </div>
              </div>

              <div className="mt-4 space-y-2">
                {aggregate.categories.map((category) => {
                  const actual = month.actualCategoryAmounts[category.code]
                  const limit = month.limitCategoryAmounts[category.code]
                  const isMonthlyLimit = isMonthlyLimitCategory(category)
                  const isOverLimit =
                    isMonthlyLimit && isPositiveAmount(limit) && compareDecimalStrings(actual, limit) > 0
                  const isTransfer = isTransferCategory(category)

                  return (
                    <div
                      key={category.code}
                      className={`rounded-2xl border px-3 py-2 ${
                        isTransfer
                          ? 'border-slate-300 bg-slate-100'
                          : isOverLimit
                          ? 'border-rose-200 bg-rose-50'
                          : 'border-slate-200 bg-white'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className={`text-sm font-medium ${isTransfer ? 'text-slate-700' : 'text-slate-900'}`}>
                          {category.label}
                        </p>
                        <p
                          className={`text-sm font-semibold ${
                            isTransfer ? 'text-slate-700' : isOverLimit ? 'text-rose-700' : 'text-slate-900'
                          }`}
                        >
                          {formatAmountOrDash(actual)}
                        </p>
                      </div>
                      {isMonthlyLimit ? (
                        <p className="mt-1 text-xs text-slate-500">Лимит {formatAmountOrDash(limit)}</p>
                      ) : null}
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
              {aggregate.categories.map((category) => (
                <div
                  key={category.code}
                  className={`rounded-2xl border px-3 py-2 ${
                    isTransferCategory(category) ? 'border-slate-300 bg-slate-100' : 'border-slate-200 bg-slate-50'
                  }`}
                >
                  <p className={`text-sm font-medium ${isTransferCategory(category) ? 'text-slate-700' : 'text-slate-900'}`}>
                    {category.label}
                  </p>
                  <p className="mt-2 text-xs text-slate-500">
                    Факт {formatAmount(aggregate.actualTotalsByCategory[category.code])}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {categoryAmountLabel(category)} {formatAmount(aggregate.limitTotalsByCategory[category.code])}
                  </p>
                </div>
              ))}
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              <MetricTile
                label="Годовой факт"
                value={formatAmountWithCurrency(aggregate.annualActualTotal, aggregate.currency)}
              />
              <MetricTile
                label="Годовой лимит расходов"
                value={formatAmountWithCurrency(aggregate.annualLimitTotal, aggregate.currency)}
              />
              <MetricTile
                label="Средний факт"
                value={formatAmountWithCurrency(aggregate.averageMonthlyActualTotal, aggregate.currency)}
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
              {aggregate.categories.map((category) => (
                <th
                  key={category.code}
                  className={`border-b border-r border-slate-200 px-3 py-3 text-left ${
                    isTransferCategory(category) ? 'bg-slate-100 text-slate-700' : 'text-slate-900'
                  }`}
                >
                  <span className="block whitespace-nowrap font-semibold">{category.label}</span>
                  <span className="mt-1 block whitespace-nowrap text-[11px] font-medium text-slate-500">
                    {limitPeriodHeaderLabel(category)}{' '}
                    {formatAmountOrDash(aggregate.limitCategoryAmounts[category.code])}
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
            {aggregate.months.map((month) => (
              <tr key={month.month} className="align-top">
                <td className="border-b border-r border-slate-200 px-2 py-3 font-semibold text-slate-900 xl:px-3">
                  {toMonthLabel(month.month)}
                </td>
                {aggregate.categories.map((category) => {
                  const actual = month.actualCategoryAmounts[category.code]
                  const limit = month.limitCategoryAmounts[category.code]
                  const isMonthlyLimit = isMonthlyLimitCategory(category)
                  const isOverLimit =
                    isMonthlyLimit && isPositiveAmount(limit) && compareDecimalStrings(actual, limit) > 0

                  return (
                    <td
                      key={category.code}
                      className={`border-b border-r border-slate-200 px-2 py-2 ${
                        isTransferCategory(category) ? 'bg-slate-100' : isOverLimit ? 'bg-rose-50' : ''
                      }`}
                    >
                      <p
                        className={`font-semibold ${
                          isTransferCategory(category)
                            ? 'text-slate-700'
                            : isOverLimit
                              ? 'text-rose-700'
                              : 'text-slate-900'
                        }`}
                      >
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
              {aggregate.categories.map((category) => (
                <td
                  key={category.code}
                  className={`border-t border-r border-slate-200 px-2 py-3 font-semibold ${
                    isTransferCategory(category) ? 'bg-slate-100 text-slate-700' : 'text-slate-900'
                  }`}
                >
                  {formatAmount(aggregate.actualTotalsByCategory[category.code])}
                </td>
              ))}
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(aggregate.annualActualTotal)}
              </td>
              <td className="border-t border-slate-200 px-2 py-3 text-slate-500">Факт за год</td>
            </tr>
            <tr>
              <td className="border-t border-r border-slate-200 px-3 py-3 font-semibold text-slate-900">
                Годовой лимит
              </td>
              {aggregate.categories.map((category) => (
                <td
                  key={category.code}
                  className={`border-t border-r border-slate-200 px-2 py-3 font-semibold ${
                    isTransferCategory(category) ? 'bg-slate-100 text-slate-700' : 'text-slate-900'
                  }`}
                >
                  {formatAmount(aggregate.limitTotalsByCategory[category.code])}
                </td>
              ))}
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(aggregate.annualLimitTotal)}
              </td>
              <td className="border-t border-slate-200 px-2 py-3 text-slate-500">Лимит за год</td>
            </tr>
            <tr>
              <td
                colSpan={aggregate.categories.length + 1}
                className="border-t border-r border-slate-200 px-3 py-3 text-sm text-slate-600"
              >
                Средний факт по заполненным месяцам
              </td>
              <td className="border-t border-r border-slate-200 px-2 py-3 font-semibold text-slate-900">
                {formatAmount(aggregate.averageMonthlyActualTotal)}
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
  cardId: string
  year: number
  currency: string
  selectedMonth: number
  initialAmount: string
  onSave: (
    cardId: string,
    month: number,
    request: UpdateMonthlyIncomeActualRequest,
  ) => Promise<boolean>
  onSaved?: () => void
}

function IncomeActualFormCard({
  cardId,
  year,
  currency,
  selectedMonth,
  initialAmount,
  onSave,
  onSaved,
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
    const saved = await onSave(cardId, selectedMonth, {
      year,
      totalAmount: toDecimalAmountString(amount),
    })

    if (saved) {
      setStatus('idle')
      onSaved?.()
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
            Сохраняется только итоговая сумма за {toMonthLabel(selectedMonth)}. Именно факт двигает review и linked balance.
          </p>
        </div>
      </div>

      <form
        className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto]"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
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

interface IncomePlanFormCardProps {
  cardId: string
  year: number
  currency: string
  salaryAmount: string
  baseForecastAmount: string
  initialIncomePlan: PersonalFinanceIncomePlanDto | null
  incomeMonths: PersonalFinanceSnapshotDto['income']['months']
  onSave: (
    cardId: string,
    year: number,
    request: UpdateIncomePlanRequest,
  ) => Promise<boolean>
  onSaved?: () => void
}

function IncomePlanFormCard({
  cardId,
  year,
  currency,
  salaryAmount,
  baseForecastAmount,
  initialIncomePlan,
  incomeMonths,
  onSave,
  onSaved,
}: IncomePlanFormCardProps) {
  const [vacations, setVacations] = useState<PersonalFinanceVacationPeriodDto[]>(() =>
    cloneVacationPeriods(initialIncomePlan?.vacations ?? []),
  )
  const [pendingStartDate, setPendingStartDate] = useState<string | null>(null)
  const [thirteenthSalaryEnabled, setThirteenthSalaryEnabled] = useState<boolean>(
    initialIncomePlan?.thirteenthSalaryEnabled ?? false,
  )
  const [thirteenthSalaryMonth, setThirteenthSalaryMonth] = useState<number>(
    initialIncomePlan?.thirteenthSalaryMonth ?? 1,
  )
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const hasBaseForecast = isPositiveAmount(baseForecastAmount)
  const plannedIncome = buildIncomePlanDraft(vacations, thirteenthSalaryEnabled, thirteenthSalaryMonth)
  const derivedPreview = deriveIncomePlanPreview(plannedIncome, salaryAmount)
  const actualMonthsWithDerivedOverrides = derivedPreview.extraMonths.filter((entry) =>
    incomeMonths.some((month) => month.month === entry.month && month.status === 'ACTUAL'),
  )
  const canSave = hasBaseForecast && pendingStartDate === null && status !== 'submitting'

  const handleDayClick = (date: string): void => {
    if (!pendingStartDate) {
      setPendingStartDate(date)
      return
    }

    const nextVacation = createOrderedVacationPeriod(pendingStartDate, date)
    setVacations((current) => sortVacationPeriods([...current, nextVacation]))
    setPendingStartDate(null)
  }

  const handleRemoveVacation = (vacationToRemove: PersonalFinanceVacationPeriodDto): void => {
    setVacations((current) =>
      current.filter(
        (vacation) =>
          vacation.startDate !== vacationToRemove.startDate || vacation.endDate !== vacationToRemove.endDate,
      ),
    )
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canSave) {
      return
    }

    setStatus('submitting')
    setErrorMessage(null)
    const saved = await onSave(cardId, year, plannedIncome)

    if (saved) {
      setStatus('idle')
      onSaved?.()
      return
    }

    setStatus('error')
    setErrorMessage('Не удалось сохранить planner доходов.')
  }

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4">
      <div>
        <div>
          <h3 className="text-base font-semibold text-slate-900">Годовой planner доходов</h3>
          <p className="mt-1 text-sm text-slate-600">
            Отметьте отпускные периоды на календаре и укажите, будет ли 13-я зарплата. Planner сам производит derived overrides по месяцам.
          </p>
        </div>
      </div>

      {!hasBaseForecast ? (
        <p className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Сначала задайте recurring forecast на вкладке настроек. Только после этого можно сохранить planner доходов.
        </p>
      ) : null}

      {pendingStartDate ? (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <p>
            Выбрана первая дата отпуска: {formatIsoDateToRu(pendingStartDate)}. Нажмите на дату окончания,
            чтобы сохранить диапазон целиком.
          </p>
          <button
            type="button"
            onClick={() => setPendingStartDate(null)}
            className="rounded-xl border border-amber-300 px-3 py-1.5 text-xs font-semibold text-amber-900"
          >
            Сбросить выбор
          </button>
        </div>
      ) : null}

      {actualMonthsWithDerivedOverrides.length > 0 ? (
        <p className="mt-4 rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-800">
          В месяцах {actualMonthsWithDerivedOverrides.map((entry) => toMonthLabel(entry.month)).join(', ')} уже есть факт.
          Review использует фактический доход, но planner сохранится и станет fallback после очистки факта.
        </p>
      ) : null}

      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          void handleSubmit(event)
        }}
      >
        <section className="rounded-2xl bg-slate-50 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Календарь отпусков</h4>
              <p className="mt-1 text-sm text-slate-600">
                Первый клик выбирает начало отпуска, второй клик выбирает конец. Touching и overlapping периоды будут объединены при расчёте payout.
              </p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Год</p>
              <p className="mt-1 text-sm font-semibold text-slate-900">{year}</p>
            </div>
          </div>

          <div className="mt-4">
            <YearVacationCalendar
              year={year}
              vacations={vacations}
              pendingStartDate={pendingStartDate}
              onSelectDate={handleDayClick}
            />
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-3">
              <h5 className="text-sm font-semibold text-slate-900">Отмеченные отпуска</h5>
              <span className="text-xs text-slate-500">{vacations.length}</span>
            </div>

            {vacations.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">
                Пока нет отпускных периодов. Для примера, отпуск `2025-06-16..2025-06-29` даст payout в июне.
              </p>
            ) : (
              <ul className="mt-3 space-y-2">
                {vacations.map((vacation) => (
                  <li
                    key={`${vacation.startDate}-${vacation.endDate}`}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 px-3 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{formatVacationPeriod(vacation)}</p>
                      <p className="mt-1 text-xs text-slate-500">
                        {getVacationLengthInDays(vacation)} календарных дней включительно
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveVacation(vacation)}
                      className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700"
                    >
                      Удалить
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <section className="rounded-2xl bg-slate-50 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">13-я зарплата</h4>
              <p className="mt-1 text-sm text-slate-600">
                Если включена, planner добавляет ещё один оклад в выбранный месяц.
              </p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-right">
              <p className="text-[11px] uppercase tracking-[0.16em] text-slate-500">Доплата</p>
              <p className="mt-1 text-sm font-semibold text-slate-900">
                {formatAmountWithCurrency(salaryAmount, currency)}
              </p>
            </div>
          </div>

          <div className="mt-4 grid gap-3 md:grid-cols-[minmax(0,1fr)_220px]">
            <label className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={thirteenthSalaryEnabled}
                onChange={(event) => setThirteenthSalaryEnabled(event.target.checked)}
                className="mt-1 h-4 w-4 rounded border-slate-300"
              />
              <span>
                <span className="font-semibold text-slate-900">Будет в этом году</span>
                <span className="mt-1 block text-xs text-slate-500">
                  Выключите, если 13-я зарплата не планируется.
                </span>
              </span>
            </label>

            <label className="text-sm text-slate-600">
              Месяц выплаты
              <select
                value={thirteenthSalaryMonth}
                disabled={!thirteenthSalaryEnabled}
                onChange={(event) => setThirteenthSalaryMonth(Number(event.target.value))}
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
        </section>

        <section className="rounded-2xl bg-slate-50 p-4">
          <div>
            <h4 className="text-sm font-semibold text-slate-900">Derived preview</h4>
            <p className="mt-1 text-sm text-slate-600">
              Preview зеркалит backend-правила: первый merged-отпуск длиной от 14 дней даёт основные отпускные в месяц его старта.
            </p>
          </div>

          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <MetricTile
              label="13-я зарплата"
              value={
                plannedIncome.thirteenthSalaryEnabled && plannedIncome.thirteenthSalaryMonth
                  ? toMonthLabel(plannedIncome.thirteenthSalaryMonth)
                  : 'Нет'
              }
              hint="Planner добавляет +оклад в выбранный месяц."
            />
            <MetricTile
              label="Первый отпуск >= 14 дней"
              value={derivedPreview.mainVacation ? formatVacationPeriod(derivedPreview.mainVacation) : 'Нет'}
              hint="Короткие отпуска сохраняются, но payout не создают."
            />
            <MetricTile
              label="Месяц отпускных"
              value={derivedPreview.vacationPayoutMonth ? toMonthLabel(derivedPreview.vacationPayoutMonth) : 'Нет'}
              hint="Используется месяц старта первого длинного отпуска."
            />
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
            <h5 className="text-sm font-semibold text-slate-900">Месяцы с дополнительным доходом</h5>
            {derivedPreview.extraMonths.length === 0 ? (
              <p className="mt-2 text-sm text-slate-500">
                Дополнительных начислений пока нет. При включённой 13-й зарплате или первом отпуске от 14 дней здесь появятся месяцы с `+ оклад`.
              </p>
            ) : (
              <ul className="mt-3 space-y-2">
                {derivedPreview.extraMonths.map((entry) => (
                  <li
                    key={entry.month}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 px-3 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{toMonthLabel(entry.month)}</p>
                      <p className="mt-1 text-xs text-slate-500">{entry.reasons.join(' + ')}</p>
                    </div>
                    <p className="text-sm font-semibold text-slate-900">
                      {formatAmountWithCurrency(entry.amount, currency)}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <p className="text-sm text-slate-600">
            Planner не создаёт транзакции и не меняет linked balance. Он только подмешивает derived overrides в месячный review.
          </p>
          <button
            type="submit"
            disabled={!canSave}
            className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {status === 'submitting' ? 'Сохраняем...' : 'Сохранить planner'}
          </button>
        </div>
      </form>

      {errorMessage ? <p className="mt-3 text-xs text-rose-600">{errorMessage}</p> : null}
      {!hasBaseForecast ? (
        <p className="mt-3 text-xs text-rose-600">
          Planner доступен только после настройки recurring forecast в настройках карты.
        </p>
      ) : null}
      {pendingStartDate ? (
        <p className="mt-3 text-xs text-rose-600">
          Завершите выбор текущего отпускного диапазона второй датой или сбросьте незавершённый выбор.
        </p>
      ) : null}
    </section>
  )
}

function YearVacationCalendar({
  year,
  vacations,
  pendingStartDate,
  onSelectDate,
}: {
  year: number
  vacations: PersonalFinanceVacationPeriodDto[]
  pendingStartDate: string | null
  onSelectDate: (date: string) => void
}) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {MONTH_LABELS.map((label, index) => (
        <VacationMonthGrid
          key={`${year}-${index + 1}`}
          year={year}
          month={index + 1}
          label={label}
          vacations={vacations}
          pendingStartDate={pendingStartDate}
          onSelectDate={onSelectDate}
        />
      ))}
    </div>
  )
}

function VacationMonthGrid({
  year,
  month,
  label,
  vacations,
  pendingStartDate,
  onSelectDate,
}: {
  year: number
  month: number
  label: string
  vacations: PersonalFinanceVacationPeriodDto[]
  pendingStartDate: string | null
  onSelectDate: (date: string) => void
}) {
  const days = daysInMonth(year, month)
  const offset = monthStartOffset(year, month)

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-3">
      <div className="flex items-center justify-between gap-3">
        <h5 className="text-sm font-semibold text-slate-900">{label}</h5>
        <span className="text-xs text-slate-400">{year}</span>
      </div>

      <div className="mt-3 grid grid-cols-7 gap-1 text-center text-[11px] uppercase tracking-[0.12em] text-slate-400">
        {DAY_LABELS.map((dayLabel) => (
          <div key={`${label}-${dayLabel}`} className="py-1">
            {dayLabel}
          </div>
        ))}
      </div>

      <div className="mt-1 grid grid-cols-7 gap-1">
        {Array.from({ length: offset }, (_, index) => (
          <div key={`empty-${label}-${index}`} className="aspect-square rounded-lg bg-slate-50/60" />
        ))}

        {Array.from({ length: days }, (_, index) => {
          const day = index + 1
          const date = formatIsoDate(year, month, day)
          const isSelected = isDateInsideVacationSelection(date, vacations)
          const isBoundary = isVacationBoundary(date, vacations)
          const isPendingStart = pendingStartDate === date
          const isWeekend = isWeekendDate(date)

          return (
            <button
              key={date}
              type="button"
              onClick={() => onSelectDate(date)}
              className={`aspect-square rounded-lg border text-xs font-medium transition ${
                isSelected
                  ? 'border-slate-900 bg-slate-900 text-white'
                  : isPendingStart
                    ? 'border-amber-300 bg-amber-100 text-amber-900'
                    : isWeekend
                      ? 'border-slate-200 bg-slate-50 text-slate-500 hover:border-slate-300'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-400'
              } ${isBoundary ? 'ring-2 ring-inset ring-cyan-300' : ''}`}
            >
              {day}
            </button>
          )
        })}
      </div>
    </section>
  )
}

function IncomeReviewTable({ aggregate }: { aggregate: AggregatedIncomeViewModel }) {
  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-3">
        <h3 className="text-base font-semibold text-slate-900">Годовая таблица доходов</h3>
        <p className="mt-1 text-sm text-slate-600">
          Суммы и статусы считаются по всем активным картам: факт, recurring forecast, planner-derived override или смешанный месяц.
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
            {aggregate.months.map((month) => (
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
                {formatAmount(aggregate.annualTotal)}
              </td>
              <td className="border-t border-slate-200 px-4 py-3 text-slate-500">Факт + прогноз + planner-derived overrides</td>
            </tr>
            <tr>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                Среднее
              </td>
              <td className="border-t border-r border-slate-200 px-4 py-3 font-semibold text-slate-900">
                {formatAmount(aggregate.averageMonthlyTotal)}
              </td>
              <td className="border-t border-slate-200 px-4 py-3 text-slate-500">По всем месяцам года</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  )
}

function StatusBadge({ status }: { status: Exclude<AggregatedIncomeMonthStatus, null> }) {
  if (status === 'ACTUAL') {
    return (
      <span className="inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
        Факт
      </span>
    )
  }

  if (status === 'MIXED') {
    return (
      <span className="inline-flex rounded-full bg-sky-50 px-2.5 py-1 text-xs font-semibold text-sky-700">
        Смешано
      </span>
    )
  }

  if (status === 'OVERRIDE') {
    return (
      <span className="inline-flex rounded-full bg-cyan-50 px-2.5 py-1 text-xs font-semibold text-cyan-700">
        Override
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

function selectableYearOptions(selectedYear: number): number[] {
  return Array.from({ length: 7 }, (_, index) => selectedYear - 3 + index)
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

interface DerivedIncomePlanExtraMonth {
  month: number
  amount: string
  reasons: string[]
}

interface DerivedIncomePlanPreview {
  normalizedVacations: PersonalFinanceVacationPeriodDto[]
  mainVacation: PersonalFinanceVacationPeriodDto | null
  thirteenthSalaryMonth: number | null
  vacationPayoutMonth: number | null
  extraMonths: DerivedIncomePlanExtraMonth[]
}

function buildIncomePlanDraft(
  vacations: PersonalFinanceVacationPeriodDto[],
  thirteenthSalaryEnabled: boolean,
  thirteenthSalaryMonth: number,
): UpdateIncomePlanRequest {
  return {
    vacations: sortVacationPeriods(cloneVacationPeriods(vacations)),
    thirteenthSalaryEnabled,
    thirteenthSalaryMonth: thirteenthSalaryEnabled ? thirteenthSalaryMonth : null,
  }
}

function cloneVacationPeriods(
  vacations: PersonalFinanceVacationPeriodDto[],
): PersonalFinanceVacationPeriodDto[] {
  return vacations.map((vacation) => ({
    startDate: vacation.startDate,
    endDate: vacation.endDate,
  }))
}

function createOrderedVacationPeriod(
  leftDate: string,
  rightDate: string,
): PersonalFinanceVacationPeriodDto {
  return leftDate <= rightDate
    ? { startDate: leftDate, endDate: rightDate }
    : { startDate: rightDate, endDate: leftDate }
}

function sortVacationPeriods(
  vacations: PersonalFinanceVacationPeriodDto[],
): PersonalFinanceVacationPeriodDto[] {
  return [...vacations].sort((left, right) =>
    left.startDate === right.startDate
      ? left.endDate.localeCompare(right.endDate)
      : left.startDate.localeCompare(right.startDate),
  )
}

function normalizeVacationPeriods(
  vacations: PersonalFinanceVacationPeriodDto[],
): PersonalFinanceVacationPeriodDto[] {
  const sorted = sortVacationPeriods(
    vacations.map((vacation) => createOrderedVacationPeriod(vacation.startDate, vacation.endDate)),
  )

  return sorted.reduce<PersonalFinanceVacationPeriodDto[]>((result, current) => {
    const lastVacation = result[result.length - 1]
    if (!lastVacation) {
      result.push(current)
      return result
    }

    const nextDayAfterLast = addDaysToIsoDate(lastVacation.endDate, 1)
    if (current.startDate <= nextDayAfterLast) {
      lastVacation.endDate = current.endDate > lastVacation.endDate ? current.endDate : lastVacation.endDate
      return result
    }

    result.push({ ...current })
    return result
  }, [])
}

function deriveIncomePlanPreview(
  incomePlan: Pick<
    PersonalFinanceIncomePlanDto,
    'vacations' | 'thirteenthSalaryEnabled' | 'thirteenthSalaryMonth'
  > | null,
  salaryAmount: string,
): DerivedIncomePlanPreview {
  const normalizedVacations = normalizeVacationPeriods(incomePlan?.vacations ?? [])
  const extraMonthsByNumber = new Map<number, DerivedIncomePlanExtraMonth>()
  const normalizedSalaryAmount = toDecimalAmountString(salaryAmount)

  if (incomePlan?.thirteenthSalaryEnabled && incomePlan.thirteenthSalaryMonth) {
    appendDerivedIncomeExtra(
      extraMonthsByNumber,
      incomePlan.thirteenthSalaryMonth,
      normalizedSalaryAmount,
      '13-я зарплата',
    )
  }

  const mainVacation =
    normalizedVacations.find((vacation) => getVacationLengthInDays(vacation) >= 14) ?? null

  if (mainVacation) {
    appendDerivedIncomeExtra(
      extraMonthsByNumber,
      getMonthFromIsoDate(mainVacation.startDate),
      normalizedSalaryAmount,
      'Основные отпускные',
    )
  }

  return {
    normalizedVacations,
    mainVacation,
    thirteenthSalaryMonth:
      incomePlan?.thirteenthSalaryEnabled && incomePlan.thirteenthSalaryMonth
        ? incomePlan.thirteenthSalaryMonth
        : null,
    vacationPayoutMonth: mainVacation ? getMonthFromIsoDate(mainVacation.startDate) : null,
    extraMonths: Array.from(extraMonthsByNumber.values()).sort((left, right) => left.month - right.month),
  }
}

function appendDerivedIncomeExtra(
  extraMonthsByNumber: Map<number, DerivedIncomePlanExtraMonth>,
  month: number,
  amount: string,
  reason: string,
): void {
  const current = extraMonthsByNumber.get(month)
  if (!current) {
    extraMonthsByNumber.set(month, {
      month,
      amount,
      reasons: [reason],
    })
    return
  }

  current.amount = addDecimalAmounts(current.amount, amount)
  current.reasons = [...current.reasons, reason]
}

function serializeIncomePlan(incomePlan: PersonalFinanceIncomePlanDto | null): string {
  if (!incomePlan) {
    return 'EMPTY'
  }

  return JSON.stringify({
    vacations: sortVacationPeriods(incomePlan.vacations),
    thirteenthSalaryEnabled: incomePlan.thirteenthSalaryEnabled,
    thirteenthSalaryMonth: incomePlan.thirteenthSalaryMonth,
  })
}

function formatVacationPeriod(vacation: PersonalFinanceVacationPeriodDto): string {
  return `${formatIsoDateToRu(vacation.startDate)} - ${formatIsoDateToRu(vacation.endDate)}`
}

function getVacationLengthInDays(vacation: PersonalFinanceVacationPeriodDto): number {
  const start = parseIsoDate(vacation.startDate)
  const end = parseIsoDate(vacation.endDate)
  return Math.floor((end.getTime() - start.getTime()) / 86_400_000) + 1
}

function formatIsoDateToRu(value: string): string {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(parseIsoDate(value))
}

function formatIsoDate(year: number, month: number, day: number): string {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map((part) => Number.parseInt(part, 10))
  return new Date(Date.UTC(year, month - 1, day))
}

function addDaysToIsoDate(value: string, days: number): string {
  const date = parseIsoDate(value)
  date.setUTCDate(date.getUTCDate() + days)
  return formatIsoDate(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate())
}

function getMonthFromIsoDate(value: string): number {
  return Number.parseInt(value.slice(5, 7), 10)
}

function isWeekendDate(value: string): boolean {
  const dayOfWeek = parseIsoDate(value).getUTCDay()
  return dayOfWeek === 0 || dayOfWeek === 6
}

function isDateInsideVacationSelection(
  value: string,
  vacations: PersonalFinanceVacationPeriodDto[],
): boolean {
  return vacations.some((vacation) => value >= vacation.startDate && value <= vacation.endDate)
}

function isVacationBoundary(
  value: string,
  vacations: PersonalFinanceVacationPeriodDto[],
): boolean {
  return vacations.some((vacation) => vacation.startDate === value || vacation.endDate === value)
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

function monthStartOffset(year: number, month: number): number {
  const dayOfWeek = new Date(Date.UTC(year, month - 1, 1)).getUTCDay()
  return dayOfWeek === 0 ? 6 : dayOfWeek - 1
}

function incomeStatusLabel(status: Exclude<PersonalFinanceSnapshotDto['income']['months'][number]['status'], null>): string {
  if (status === 'ACTUAL') {
    return 'Факт'
  }

  if (status === 'OVERRIDE') {
    return 'Override'
  }

  return 'Прогноз'
}

function isMonthlyLimitCategory(category: PersonalExpenseCategoryDto): boolean {
  return category.limitPeriod === 'MONTHLY' && !isTransferCategory(category)
}

function isTransferCategory(category: PersonalExpenseCategoryDto): boolean {
  return category.classification === 'TRANSFER'
}

function limitPercentInputLabel(category: PersonalExpenseCategoryDto): string {
  if (isTransferCategory(category)) {
    return '% цели от прогноза за год'
  }

  return category.limitPeriod === 'ANNUAL' ? '% от прогноза за год' : '% от прогноза за месяц'
}

function limitPeriodHeaderLabel(category: PersonalExpenseCategoryDto): string {
  if (isTransferCategory(category)) {
    return 'Цель за год'
  }

  return category.limitPeriod === 'ANNUAL' ? 'За год' : 'Лимит'
}

function categoryAmountLabel(category: PersonalExpenseCategoryDto): string {
  return isTransferCategory(category) ? 'Цель' : 'Лимит'
}

function calculateConfiguredLimitAmount(
  category: PersonalExpenseCategoryDto,
  percentValue: string,
  monthlyForecast: string,
): string {
  const baseAmount =
    category.limitPeriod === 'ANNUAL' ? multiplyDecimalAmount(monthlyForecast, 12) : toDecimalAmountString(monthlyForecast)

  return calculatePercentAmount(baseAmount, percentValue)
}

function calculateConfiguredLimitTotals(
  categories: PersonalExpenseCategoryDto[],
  percentValues: Record<PersonalExpenseCategoryCode, string>,
  monthlyForecast: string,
): { monthlyLimitTotal: string; annualLimitTotal: string } {
  const monthlyValues: string[] = []
  const annualValues: string[] = []

  categories.forEach((category) => {
    const amount = calculateConfiguredLimitAmount(category, percentValues[category.code], monthlyForecast)
    if (isTransferCategory(category)) {
      return
    }

    if (category.limitPeriod === 'ANNUAL') {
      annualValues.push(amount)
      return
    }

    monthlyValues.push(amount)
  })

  return {
    monthlyLimitTotal: sumDecimalAmountStrings(monthlyValues),
    annualLimitTotal: sumDecimalAmountStrings([
      ...annualValues,
      ...monthlyValues.map((value) => multiplyDecimalAmount(value, 12)),
    ]),
  }
}

function calculatePercentAmount(baseAmount: string, percentValue: string): string {
  const base = Number.parseFloat(toDecimalAmountString(baseAmount))
  const percent = Number.parseFloat(toDecimalAmountString(percentValue))
  return ((base * percent) / 100).toFixed(2)
}

function multiplyDecimalAmount(value: string, factor: number): string {
  return (Number.parseFloat(value || '0') * factor).toFixed(2)
}

function aggregateExpenses(activeSnapshots: PersonalFinanceSnapshotDto[]): AggregatedExpensesViewModel {
  const [firstSnapshot] = activeSnapshots
  const categories = firstSnapshot.categories
  const months = Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    actualCategoryAmounts: createZeroAmountMap(categories),
    limitCategoryAmounts: createZeroAmountMap(categories),
    actualTotal: '0.00',
    limitTotal: '0.00',
  }))
  const limitCategoryAmounts = createZeroAmountMap(categories)
  const actualTotalsByCategory = createZeroAmountMap(categories)
  const limitTotalsByCategory = createZeroAmountMap(categories)

  activeSnapshots.forEach((snapshot) => {
    categories.forEach((category) => {
      limitCategoryAmounts[category.code] = addDecimalAmounts(
        limitCategoryAmounts[category.code],
        snapshot.settings.limitCategoryAmounts[category.code],
      )
      actualTotalsByCategory[category.code] = addDecimalAmounts(
        actualTotalsByCategory[category.code],
        snapshot.expenses.actualTotalsByCategory[category.code],
      )
      limitTotalsByCategory[category.code] = addDecimalAmounts(
        limitTotalsByCategory[category.code],
        snapshot.expenses.limitTotalsByCategory[category.code],
      )
    })

    snapshot.expenses.months.forEach((month) => {
      const aggregateMonth = months[month.month - 1]
      categories.forEach((category) => {
        aggregateMonth.actualCategoryAmounts[category.code] = addDecimalAmounts(
          aggregateMonth.actualCategoryAmounts[category.code],
          month.actualCategoryAmounts[category.code],
        )
        aggregateMonth.limitCategoryAmounts[category.code] = addDecimalAmounts(
          aggregateMonth.limitCategoryAmounts[category.code],
          month.limitCategoryAmounts[category.code],
        )
      })
      aggregateMonth.actualTotal = addDecimalAmounts(aggregateMonth.actualTotal, month.actualTotal)
      aggregateMonth.limitTotal = addDecimalAmounts(aggregateMonth.limitTotal, month.limitTotal)
    })
  })

  const annualActualTotal = sumDecimalAmountStrings(months.map((month) => month.actualTotal))
  const annualLimitTotal = sumDecimalAmountStrings(
    activeSnapshots.map((snapshot) => snapshot.expenses.annualLimitTotal),
  )
  const filledMonths = months.filter((month) => isPositiveAmount(month.actualTotal)).length

  return {
    categories,
    currency: firstSnapshot.currency,
    limitCategoryAmounts,
    months,
    actualTotalsByCategory,
    limitTotalsByCategory,
    annualActualTotal,
    annualLimitTotal,
    averageMonthlyActualTotal: averageDecimalAmounts(annualActualTotal, filledMonths),
  }
}

function aggregateIncome(activeSnapshots: PersonalFinanceSnapshotDto[]): AggregatedIncomeViewModel {
  const [firstSnapshot] = activeSnapshots
  const months: AggregatedIncomeMonthViewModel[] = Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    totalAmount: '0.00',
    status: null,
  }))

  let cardsWithForecast = 0

  activeSnapshots.forEach((snapshot) => {
    const forecastAmount = snapshot.settings.incomeForecast?.totalAmount ?? '0.00'
    if (isPositiveAmount(forecastAmount)) {
      cardsWithForecast += 1
    }

    snapshot.income.months.forEach((month) => {
      const aggregateMonth = months[month.month - 1]
      aggregateMonth.totalAmount = addDecimalAmounts(aggregateMonth.totalAmount, month.totalAmount)
      aggregateMonth.status = mergeIncomeStatuses(aggregateMonth.status, month.status)
    })
  })

  const annualTotal = sumDecimalAmountStrings(months.map((month) => month.totalAmount))
  const filledMonths = months.filter((month) => isPositiveAmount(month.totalAmount)).length

  return {
    currency: firstSnapshot.currency,
    months,
    annualTotal,
    averageMonthlyTotal: averageDecimalAmounts(annualTotal, filledMonths),
    recurringForecast: {
      totalAmount: sumDecimalAmountStrings(
        activeSnapshots.map((snapshot) => snapshot.settings.incomeForecast?.totalAmount ?? '0.00'),
      ),
      activeCardCount: activeSnapshots.length,
      cardsWithForecast,
    },
  }
}

function resolveDefaultActiveCardId(
  activeSnapshots: PersonalFinanceSnapshotDto[],
  preferredCardId: string | null,
): string {
  if (preferredCardId && activeSnapshots.some((snapshot) => snapshot.card.id === preferredCardId)) {
    return preferredCardId
  }

  return activeSnapshots[0]?.card.id ?? ''
}

function createZeroAmountMap(
  categories: PersonalExpenseCategoryDto[],
): Record<PersonalExpenseCategoryCode, string> {
  return categories.reduce(
    (result, category) => ({
      ...result,
      [category.code]: '0.00',
    }),
    {} as Record<PersonalExpenseCategoryCode, string>,
  )
}

function addDecimalAmounts(left: string, right: string): string {
  return sumDecimalAmountStrings([left, right])
}

function averageDecimalAmounts(total: string, count: number): string {
  if (count === 0) {
    return '0.00'
  }

  return (Number.parseFloat(total) / count).toFixed(2)
}

function mergeIncomeStatuses(
  current: AggregatedIncomeMonthStatus,
  next: PersonalFinanceSnapshotDto['income']['months'][number]['status'],
): AggregatedIncomeMonthStatus {
  if (!next) {
    return current
  }

  if (!current) {
    return next
  }

  if (current === 'MIXED' || current !== next) {
    return 'MIXED'
  }

  return current
}
