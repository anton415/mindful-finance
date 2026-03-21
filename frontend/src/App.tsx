import { startTransition, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  ApiClientError,
  apiClient,
  type AccountDto,
  type AccountType,
  type CreatePersonalFinanceCardRequest,
  type CreateAccountRequest,
  type CreateTransactionRequest,
  type CurrencyTotalsDto,
  type ImportTransactionsCsvResponse,
  type MoneyDto,
  type PersonalFinanceCardDto,
  type PersonalFinanceSnapshotDto,
  type TransactionDirection,
  type TransactionDto,
  type UpdateMonthlyExpenseRequest,
  type UpdateMonthlyIncomeActualRequest,
  type UpdatePersonalFinanceCardRequest,
  type UpdatePersonalFinanceSettingsRequest,
  type UpdateTransactionRequest,
} from './api'
import {
  PersonalFinanceView,
  type PersonalFinanceCardListItem,
  type PersonalFinanceTab,
} from './features/personal-finance/PersonalFinanceView'

interface DashboardData {
  asOf: string
  netWorth: CurrencyTotalsDto
  monthlyBurn: CurrencyTotalsDto
  monthlySavings: CurrencyTotalsDto
}

interface AccountWithBalance extends AccountDto {
  balance: MoneyDto
}

type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
type ViewTab = 'dashboard' | 'accounts' | 'personal-finance'
type TransactionDirectionFilter = 'ALL' | 'INFLOW' | 'OUTFLOW'
type CreateAccountStatus = 'idle' | 'submitting' | 'error'
type CreateTransactionStatus = 'idle' | 'submitting' | 'error'
type CsvImportStatus = 'idle' | 'submitting' | 'success' | 'error'
type AccountStatus = 'ACTIVE' | string
interface NavigationState {
  tab: ViewTab
  accountId: string | null
  financeTab: PersonalFinanceTab
  financeYear: number
  financeCardId: string | null
}

const ACCOUNT_TYPE_OPTIONS: AccountType[] = ['CASH', 'DEPOSIT', 'FUND', 'IIS', 'BROKERAGE']
const ACCOUNT_CURRENCY_OPTIONS: string[] = getAccountCurrencyOptions()
const DEFAULT_ACCOUNT_CURRENCY =
  ACCOUNT_CURRENCY_OPTIONS.includes('USD') && ACCOUNT_CURRENCY_OPTIONS.length > 0
    ? 'USD'
    : (ACCOUNT_CURRENCY_OPTIONS[0] ?? 'USD')
const DEFAULT_NAVIGATION_STATE: NavigationState = {
  tab: 'dashboard',
  accountId: null,
  financeTab: 'expenses',
  financeYear: currentYear(),
  financeCardId: null,
}

function App() {
  const [activeView, setActiveView] = useState<ViewTab>(() => readNavigationFromUrl().tab)
  const [activePersonalFinanceTab, setActivePersonalFinanceTab] = useState<PersonalFinanceTab>(
    () => readNavigationFromUrl().financeTab,
  )
  const [selectedPersonalFinanceYear, setSelectedPersonalFinanceYear] = useState<number>(
    () => readNavigationFromUrl().financeYear,
  )
  const [selectedPersonalFinanceCardId, setSelectedPersonalFinanceCardId] = useState<string | null>(
    () => readNavigationFromUrl().financeCardId,
  )

  const [dashboardStatus, setDashboardStatus] = useState<LoadStatus>('idle')
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [dashboardErrorMessage, setDashboardErrorMessage] = useState<string | null>(null)
  const [dashboardReloadTick, setDashboardReloadTick] = useState<number>(0)

  const [accountsStatus, setAccountsStatus] = useState<LoadStatus>('idle')
  const [accounts, setAccounts] = useState<AccountWithBalance[]>([])
  const [accountsErrorMessage, setAccountsErrorMessage] = useState<string | null>(null)
  const [accountsReloadTick, setAccountsReloadTick] = useState<number>(0)
  const [createAccountStatus, setCreateAccountStatus] = useState<CreateAccountStatus>('idle')
  const [createAccountErrorMessage, setCreateAccountErrorMessage] = useState<string | null>(null)

  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(
    () => readNavigationFromUrl().accountId,
  )
  const [transactionsStatus, setTransactionsStatus] = useState<LoadStatus>('idle')
  const [transactions, setTransactions] = useState<TransactionDto[]>([])
  const [transactionsErrorMessage, setTransactionsErrorMessage] = useState<string | null>(null)
  const [transactionsReloadTick, setTransactionsReloadTick] = useState<number>(0)
  const [createTransactionStatus, setCreateTransactionStatus] = useState<CreateTransactionStatus>('idle')
  const [createTransactionErrorMessage, setCreateTransactionErrorMessage] = useState<string | null>(null)
  const [csvImportStatus, setCsvImportStatus] = useState<CsvImportStatus>('idle')
  const [csvImportErrorMessage, setCsvImportErrorMessage] = useState<string | null>(null)
  const [csvImportResult, setCsvImportResult] = useState<ImportTransactionsCsvResponse | null>(null)
  const [personalFinanceStatus, setPersonalFinanceStatus] = useState<LoadStatus>('idle')
  const [personalFinanceCards, setPersonalFinanceCards] = useState<PersonalFinanceCardListItem[]>([])
  const [activePersonalFinanceSnapshots, setActivePersonalFinanceSnapshots] = useState<
    PersonalFinanceSnapshotDto[]
  >([])
  const [selectedPersonalFinanceSettingsSnapshot, setSelectedPersonalFinanceSettingsSnapshot] =
    useState<PersonalFinanceSnapshotDto | null>(null)
  const [personalFinanceErrorMessage, setPersonalFinanceErrorMessage] = useState<string | null>(null)
  const [personalFinanceReloadTick, setPersonalFinanceReloadTick] = useState<number>(0)

  const [directionFilter, setDirectionFilter] = useState<TransactionDirectionFilter>('ALL')
  const [memoFilter, setMemoFilter] = useState<string>('')

  useEffect(() => {
    syncNavigationToUrl(
      activeView,
      selectedAccountId,
      activePersonalFinanceTab,
      selectedPersonalFinanceYear,
      selectedPersonalFinanceCardId,
    )
  }, [
    activeView,
    selectedAccountId,
    activePersonalFinanceTab,
    selectedPersonalFinanceYear,
    selectedPersonalFinanceCardId,
  ])

  useEffect(() => {
    const controller = new AbortController()

    const loadDashboard = async (): Promise<void> => {
      const asOf = todayIsoDate()
      setDashboardStatus('loading')
      setDashboardErrorMessage(null)

      try {
        const [netWorth, monthlyBurn, monthlySavings] = await Promise.all([
          apiClient.getNetWorth(controller.signal),
          apiClient.getMonthlyBurn({ asOf, signal: controller.signal }),
          apiClient.getMonthlySavings({ asOf, signal: controller.signal }),
        ])

        if (controller.signal.aborted) {
          return
        }

        setDashboard({
          asOf,
          netWorth,
          monthlyBurn,
          monthlySavings,
        })
        setDashboardStatus('ready')
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setDashboardStatus('error')
        setDashboardErrorMessage(toErrorMessage(error))
      }
    }

    void loadDashboard()

    return () => {
      controller.abort()
    }
  }, [dashboardReloadTick])

  useEffect(() => {
    if (activeView !== 'accounts') {
      return
    }

    const controller = new AbortController()

    const loadAccounts = async (): Promise<void> => {
      setAccountsStatus('loading')
      setAccountsErrorMessage(null)

      try {
        const accountRows = await apiClient.listAccounts(controller.signal)
        const balances = await Promise.all(
          accountRows.map((account) => apiClient.getAccountBalance(account.id, controller.signal)),
        )

        if (controller.signal.aborted) {
          return
        }

        const rows = accountRows
          .map((account, index) => ({
            ...account,
            balance: balances[index],
          }))
          .sort((left, right) => left.name.localeCompare(right.name))

        setAccounts(rows)
        setSelectedAccountId((currentSelection) => {
          if (rows.length === 0) {
            return null
          }

          if (currentSelection && rows.some((account) => account.id === currentSelection)) {
            return currentSelection
          }

          return rows[0].id
        })
        setAccountsStatus('ready')
        setCreateAccountStatus('idle')
        setCreateAccountErrorMessage(null)
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setAccountsStatus('error')
        setAccountsErrorMessage(toErrorMessage(error))
      }
    }

    void loadAccounts()

    return () => {
      controller.abort()
    }
  }, [activeView, accountsReloadTick])

  useEffect(() => {
    if (
      activeView !== 'accounts' ||
      !selectedAccountId ||
      !accounts.some((account) => account.id === selectedAccountId)
    ) {
      return
    }

    const controller = new AbortController()

    const loadTransactions = async (): Promise<void> => {
      setTransactionsStatus('loading')
      setTransactionsErrorMessage(null)

      try {
        const rows = await apiClient.listAccountTransactions(selectedAccountId, controller.signal)

        if (controller.signal.aborted) {
          return
        }

        const sortedRows = [...rows].sort((left, right) => right.occurredOn.localeCompare(left.occurredOn))
        setTransactions(sortedRows)
        setTransactionsStatus('ready')
        setCreateTransactionStatus('idle')
        setCreateTransactionErrorMessage(null)
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setTransactionsStatus('error')
        setTransactionsErrorMessage(toErrorMessage(error))
      }
    }

    void loadTransactions()

    return () => {
      controller.abort()
    }
  }, [activeView, accounts, selectedAccountId, transactionsReloadTick])

  useEffect(() => {
    if (activeView !== 'personal-finance') {
      return
    }

    const controller = new AbortController()

    const loadPersonalFinance = async (): Promise<void> => {
      setPersonalFinanceStatus('loading')
      setPersonalFinanceErrorMessage(null)

      try {
        const cards = await apiClient.listPersonalFinanceCards(controller.signal)
        const activeCards = cards.filter((card) => card.status === 'ACTIVE')

        if (controller.signal.aborted) {
          return
        }

        if (cards.length === 0) {
          setPersonalFinanceCards([])
          setActivePersonalFinanceTab('settings')
          setSelectedPersonalFinanceCardId(null)
          setActivePersonalFinanceSnapshots([])
          setSelectedPersonalFinanceSettingsSnapshot(null)
          setPersonalFinanceStatus('ready')
          return
        }

        const resolvedCardId = resolvePersonalFinanceSelection(cards, selectedPersonalFinanceCardId)
        if (resolvedCardId !== selectedPersonalFinanceCardId) {
          setSelectedPersonalFinanceCardId(resolvedCardId)
        }

        const activeSnapshotResults = await Promise.allSettled(
          activeCards.map((card) =>
            apiClient.getPersonalFinanceSnapshot(card.id, selectedPersonalFinanceYear, controller.signal),
          ),
        )

        if (controller.signal.aborted) {
          return
        }

        const activeSnapshots = activeSnapshotResults.flatMap((result) =>
          result.status === 'fulfilled' ? [result.value] : [],
        )
        const settingsCardId = resolvePersonalFinanceSettingsSelection(
          cards,
          resolvedCardId,
          activeSnapshots,
        )

        if (settingsCardId !== selectedPersonalFinanceCardId) {
          setSelectedPersonalFinanceCardId(settingsCardId)
        }

        let settingsSnapshot =
          activeSnapshots.find((snapshot) => snapshot.card.id === settingsCardId) ?? null

        if (!settingsSnapshot && settingsCardId) {
          const settingsSnapshotResult = await Promise.allSettled([
            apiClient.getPersonalFinanceSnapshot(
              settingsCardId,
              selectedPersonalFinanceYear,
              controller.signal,
            ),
          ])
          const fulfilledSettingsSnapshot = settingsSnapshotResult[0]
          if (fulfilledSettingsSnapshot?.status === 'fulfilled') {
            settingsSnapshot = fulfilledSettingsSnapshot.value
          }
        }

        if (controller.signal.aborted) {
          return
        }

        const mergedActiveSnapshots =
          settingsSnapshot &&
          settingsSnapshot.card.status === 'ACTIVE' &&
          !activeSnapshots.some((snapshot) => snapshot.card.id === settingsSnapshot.card.id)
            ? [...activeSnapshots, settingsSnapshot]
            : activeSnapshots

        setActivePersonalFinanceSnapshots(mergedActiveSnapshots)
        setSelectedPersonalFinanceSettingsSnapshot(settingsSnapshot)
        setPersonalFinanceCards(
          enrichPersonalFinanceCards(cards, buildPersonalFinanceCardSummary(mergedActiveSnapshots, settingsSnapshot)),
        )
        setPersonalFinanceStatus('ready')
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setPersonalFinanceStatus('error')
        setPersonalFinanceErrorMessage(toErrorMessage(error))
      }
    }

    void loadPersonalFinance()

    return () => {
      controller.abort()
    }
  }, [activeView, selectedPersonalFinanceCardId, selectedPersonalFinanceYear, personalFinanceReloadTick])

  const selectedAccount = selectedAccountId
    ? accounts.find((account) => account.id === selectedAccountId) ?? null
    : null

  const filteredTransactions = useMemo(() => {
    const normalizedMemoFilter = memoFilter.trim().toLowerCase()

    return transactions.filter((transaction) => {
      if (directionFilter !== 'ALL' && transaction.direction !== directionFilter) {
        return false
      }

      if (normalizedMemoFilter.length > 0) {
        return normalizeTransactionMemo(transaction.memo).toLowerCase().includes(normalizedMemoFilter)
      }

      return true
    })
  }, [transactions, directionFilter, memoFilter])

  const localizedTitle = getViewTitle(activeView)
  const localizedDescription = getViewDescription(activeView)
  const headerContextLabel =
    activeView === 'personal-finance'
      ? activePersonalFinanceTab === 'settings'
        ? selectedPersonalFinanceSettingsSnapshot?.card
          ? `${selectedPersonalFinanceSettingsSnapshot.card.name} · ${selectedPersonalFinanceYear}`
          : `Год ${selectedPersonalFinanceYear}`
        : `Все активные карты · ${selectedPersonalFinanceYear}`
      : dashboard
        ? `На дату ${formatIsoDateRu(dashboard.asOf)}`
        : 'На сегодня'

  const handleAccountSelect = (accountId: string): void => {
    if (accountId === selectedAccountId) {
      setTransactionsReloadTick((tick) => tick + 1)
      return
    }

    setSelectedAccountId(accountId)
    setDirectionFilter('ALL')
    setMemoFilter('')
    setTransactions([])
    setTransactionsErrorMessage(null)
    setCreateTransactionStatus('idle')
    setCreateTransactionErrorMessage(null)
    setCsvImportStatus('idle')
    setCsvImportErrorMessage(null)
    setCsvImportResult(null)
  }

  const handleCreateAccount = async (request: CreateAccountRequest): Promise<boolean> => {
    setCreateAccountStatus('submitting')
    setCreateAccountErrorMessage(null)

    try {
      const created = await apiClient.createAccount(request)
      setSelectedAccountId(created.accountId)
      setAccountsReloadTick((tick) => tick + 1)
      setCreateAccountStatus('idle')
      setCreateAccountErrorMessage(null)
      return true
    } catch (error) {
      setCreateAccountStatus('error')
      setCreateAccountErrorMessage(toErrorMessage(error))
      return false
    }
  }

  const refreshTransactionDerivedViews = (): void => {
    setTransactionsReloadTick((tick) => tick + 1)
    setAccountsReloadTick((tick) => tick + 1)
    setDashboardReloadTick((tick) => tick + 1)
  }

  const refreshPersonalFinanceDerivedViews = (): void => {
    setPersonalFinanceReloadTick((tick) => tick + 1)
    setAccountsReloadTick((tick) => tick + 1)
    setTransactionsReloadTick((tick) => tick + 1)
    setDashboardReloadTick((tick) => tick + 1)
  }

  const handleCreateTransaction = async (
    accountId: string,
    request: CreateTransactionRequest,
  ): Promise<boolean> => {
    setCreateTransactionStatus('submitting')
    setCreateTransactionErrorMessage(null)

    try {
      await apiClient.createTransaction(accountId, request)
      setCreateTransactionStatus('idle')
      setCreateTransactionErrorMessage(null)
      refreshTransactionDerivedViews()
      return true
    } catch (error) {
      setCreateTransactionStatus('error')
      setCreateTransactionErrorMessage(toErrorMessage(error))
      return false
    }
  }

  const handleImportTransactionsCsv = async (accountId: string, file: File): Promise<boolean> => {
    setCsvImportStatus('submitting')
    setCsvImportErrorMessage(null)
    setCsvImportResult(null)

    try {
      const response = await apiClient.importTransactionsCsv(accountId, file)
      setCsvImportStatus('success')
      setCsvImportErrorMessage(null)
      setCsvImportResult(response)
      refreshTransactionDerivedViews()
      return true
    } catch (error) {
      setCsvImportStatus('error')
      setCsvImportErrorMessage(toErrorMessage(error))
      setCsvImportResult(null)
      return false
    }
  }

  const handleUpdateTransaction = async (
    accountId: string,
    transactionId: string,
    request: UpdateTransactionRequest,
  ): Promise<void> => {
    await apiClient.updateTransaction(accountId, transactionId, request)
    refreshTransactionDerivedViews()
  }

  const handleDeleteTransaction = async (
    accountId: string,
    transactionId: string,
  ): Promise<void> => {
    await apiClient.deleteTransaction(accountId, transactionId)
    setTransactions((current) => current.filter((transaction) => transaction.id !== transactionId))
    startTransition(() => {
      refreshTransactionDerivedViews()
    })
  }

  const handleCreatePersonalFinanceCard = async (
    request: CreatePersonalFinanceCardRequest,
  ): Promise<boolean> => {
    setActivePersonalFinanceTab('settings')
    setPersonalFinanceErrorMessage(null)

    try {
      const created = await apiClient.createPersonalFinanceCard(request)
      setSelectedPersonalFinanceCardId(created.cardId)
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleSaveExpenseActual = async (
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
  ): Promise<boolean> => {
    try {
      await apiClient.updateMonthlyExpenseActual(cardId, month, request)
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleSaveIncomeActual = async (
    cardId: string,
    month: number,
    request: UpdateMonthlyIncomeActualRequest,
  ): Promise<boolean> => {
    if (cardId.trim().length === 0) {
      return false
    }

    try {
      await apiClient.updateMonthlyIncomeActual(
        cardId,
        month,
        request,
      )
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleSavePersonalFinanceSettings = async (
    request: UpdatePersonalFinanceSettingsRequest,
  ): Promise<boolean> => {
    if (!selectedPersonalFinanceCardId) {
      return false
    }

    try {
      await apiClient.updatePersonalFinanceSettings(selectedPersonalFinanceCardId, request)
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleRenamePersonalFinanceCard = async (
    request: UpdatePersonalFinanceCardRequest,
  ): Promise<boolean> => {
    if (!selectedPersonalFinanceCardId) {
      return false
    }

    try {
      await apiClient.updatePersonalFinanceCard(selectedPersonalFinanceCardId, request)
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleArchivePersonalFinanceCard = async (cardId: string): Promise<boolean> => {
    try {
      await apiClient.archivePersonalFinanceCard(cardId)
      setSelectedPersonalFinanceCardId(null)
      setActivePersonalFinanceTab('settings')
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleRestorePersonalFinanceCard = async (cardId: string): Promise<boolean> => {
    try {
      await apiClient.restorePersonalFinanceCard(cardId)
      setSelectedPersonalFinanceCardId(cardId)
      setActivePersonalFinanceTab('settings')
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleDeletePersonalFinanceCard = async (cardId: string): Promise<boolean> => {
    try {
      await apiClient.deletePersonalFinanceCard(cardId)
      setSelectedPersonalFinanceCardId((current) => (current === cardId ? null : current))
      setActivePersonalFinanceTab('settings')
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  return (
    <main className="min-h-screen w-full px-4 py-8 sm:px-6 sm:py-14">
      <section className="rounded-2xl border border-slate-200 bg-white/85 p-5 shadow-sm backdrop-blur lg:p-8">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium uppercase tracking-[0.2em] text-slate-500">
              Mindful Finance
            </p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-900">{localizedTitle}</h1>
            <p className="mt-3 max-w-2xl text-sm leading-relaxed text-slate-600">
              {localizedDescription}
            </p>
          </div>
          <p className="text-xs uppercase tracking-wide text-slate-500">{headerContextLabel}</p>
        </header>

        <nav className="mt-8 inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1">
          <TabButton
            label="Обзор"
            isActive={activeView === 'dashboard'}
            onClick={() => setActiveView('dashboard')}
          />
          <TabButton
            label="Инвестиции"
            isActive={activeView === 'accounts'}
            onClick={() => setActiveView('accounts')}
          />
          <TabButton
            label="Личные финансы"
            isActive={activeView === 'personal-finance'}
            onClick={() => setActiveView('personal-finance')}
          />
        </nav>

        <section className="mt-6">
          {activeView === 'dashboard' ? (
            <DashboardView
              status={dashboardStatus}
              dashboard={dashboard}
              errorMessage={dashboardErrorMessage}
              onRetry={() => {
                setDashboardErrorMessage(null)
                setDashboardReloadTick((tick) => tick + 1)
              }}
            />
          ) : activeView === 'personal-finance' ? (
            <PersonalFinanceView
              status={personalFinanceStatus}
              cards={personalFinanceCards}
              activeSnapshots={activePersonalFinanceSnapshots}
              settingsSnapshot={selectedPersonalFinanceSettingsSnapshot}
              selectedCardId={selectedPersonalFinanceCardId}
              activeTab={activePersonalFinanceTab}
              year={selectedPersonalFinanceYear}
              errorMessage={personalFinanceErrorMessage}
              onSelectTab={setActivePersonalFinanceTab}
              onSelectYear={setSelectedPersonalFinanceYear}
              onSelectCard={setSelectedPersonalFinanceCardId}
              onRetry={() => {
                setPersonalFinanceErrorMessage(null)
                setPersonalFinanceReloadTick((tick) => tick + 1)
              }}
              onCreateCard={handleCreatePersonalFinanceCard}
              onSaveExpenseActual={handleSaveExpenseActual}
              onSaveIncomeActual={handleSaveIncomeActual}
              onRenameCard={handleRenamePersonalFinanceCard}
              onSaveSettings={handleSavePersonalFinanceSettings}
              onArchiveCard={handleArchivePersonalFinanceCard}
              onRestoreCard={handleRestorePersonalFinanceCard}
              onDeleteCard={handleDeletePersonalFinanceCard}
            />
          ) : (
            <AccountsView
              status={accountsStatus}
              accounts={accounts}
              selectedAccountId={selectedAccountId}
              selectedAccount={selectedAccount}
              transactionsStatus={transactionsStatus}
              transactionsErrorMessage={transactionsErrorMessage}
              filteredTransactions={filteredTransactions}
              totalTransactionsCount={transactions.length}
              directionFilter={directionFilter}
              memoFilter={memoFilter}
              onDirectionFilterChange={setDirectionFilter}
              onMemoFilterChange={setMemoFilter}
              onSelectAccount={handleAccountSelect}
              createAccountStatus={createAccountStatus}
              createAccountErrorMessage={createAccountErrorMessage}
              onCreateAccount={handleCreateAccount}
              createTransactionStatus={createTransactionStatus}
              createTransactionErrorMessage={createTransactionErrorMessage}
              onCreateTransaction={handleCreateTransaction}
              onUpdateTransaction={handleUpdateTransaction}
              onDeleteTransaction={handleDeleteTransaction}
              csvImportStatus={csvImportStatus}
              csvImportErrorMessage={csvImportErrorMessage}
              csvImportResult={csvImportResult}
              onImportTransactionsCsv={handleImportTransactionsCsv}
              errorMessage={accountsErrorMessage}
              onRetryAccounts={() => {
                setAccountsErrorMessage(null)
                setAccountsReloadTick((tick) => tick + 1)
              }}
              onRetryTransactions={() => {
                setTransactionsReloadTick((tick) => tick + 1)
              }}
            />
          )}
        </section>
      </section>
    </main>
  )
}

interface DashboardViewProps {
  status: LoadStatus
  dashboard: DashboardData | null
  errorMessage: string | null
  onRetry: () => void
}

function DashboardView({ status, dashboard, errorMessage, onRetry }: DashboardViewProps) {
  if (status === 'loading' || status === 'idle') {
    return <StatusCard tone="neutral" message="Загружаем метрики обзора..." />
  }

  if (status === 'error') {
    return (
      <StatusCard
        tone="warning"
        message={errorMessage ?? 'Не удалось загрузить метрики обзора.'}
        actionLabel="Повторить"
        onAction={onRetry}
      />
    )
  }

  if (!dashboard) {
    return <StatusCard tone="neutral" message="Пока нет данных для обзора." />
  }

  return (
    <div className="space-y-4">
      <p className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
        Общие метрики учитывают и инвестиционные счета, и активные карты из раздела личных финансов.
      </p>

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard title="Капитал" subtitle="С группировкой по валютам" totals={dashboard.netWorth} />
        <MetricCard
          title="Месячный расход"
          subtitle="Сумма расходов за последние 30 дней"
          totals={dashboard.monthlyBurn}
        />
        <MetricCard
          title="Месячные накопления"
          subtitle="Доходы минус расходы за последние 30 дней"
          totals={dashboard.monthlySavings}
        />
      </div>
    </div>
  )
}

interface AccountsViewProps {
  status: LoadStatus
  accounts: AccountWithBalance[]
  selectedAccountId: string | null
  selectedAccount: AccountWithBalance | null
  transactionsStatus: LoadStatus
  transactionsErrorMessage: string | null
  filteredTransactions: TransactionDto[]
  totalTransactionsCount: number
  directionFilter: TransactionDirectionFilter
  memoFilter: string
  onDirectionFilterChange: (value: TransactionDirectionFilter) => void
  onMemoFilterChange: (value: string) => void
  onSelectAccount: (accountId: string) => void
  createAccountStatus: CreateAccountStatus
  createAccountErrorMessage: string | null
  onCreateAccount: (request: CreateAccountRequest) => Promise<boolean>
  createTransactionStatus: CreateTransactionStatus
  createTransactionErrorMessage: string | null
  onCreateTransaction: (accountId: string, request: CreateTransactionRequest) => Promise<boolean>
  onUpdateTransaction: (
    accountId: string,
    transactionId: string,
    request: UpdateTransactionRequest,
  ) => Promise<void>
  onDeleteTransaction: (accountId: string, transactionId: string) => Promise<void>
  csvImportStatus: CsvImportStatus
  csvImportErrorMessage: string | null
  csvImportResult: ImportTransactionsCsvResponse | null
  onImportTransactionsCsv: (accountId: string, file: File) => Promise<boolean>
  errorMessage: string | null
  onRetryAccounts: () => void
  onRetryTransactions: () => void
}

function AccountsView({
  status,
  accounts,
  selectedAccountId,
  selectedAccount,
  transactionsStatus,
  transactionsErrorMessage,
  filteredTransactions,
  totalTransactionsCount,
  directionFilter,
  memoFilter,
  onDirectionFilterChange,
  onMemoFilterChange,
  onSelectAccount,
  createAccountStatus,
  createAccountErrorMessage,
  onCreateAccount,
  createTransactionStatus,
  createTransactionErrorMessage,
  onCreateTransaction,
  onUpdateTransaction,
  onDeleteTransaction,
  csvImportStatus,
  csvImportErrorMessage,
  csvImportResult,
  onImportTransactionsCsv,
  errorMessage,
  onRetryAccounts,
  onRetryTransactions,
}: AccountsViewProps) {
  const [newAccountName, setNewAccountName] = useState<string>('')
  const [newAccountCurrency, setNewAccountCurrency] = useState<string>(DEFAULT_ACCOUNT_CURRENCY)
  const [newAccountType, setNewAccountType] = useState<AccountType>('CASH')
  const [newTransactionDate, setNewTransactionDate] = useState<string>(todayIsoDate())
  const [newTransactionDirection, setNewTransactionDirection] = useState<TransactionDirection>('OUTFLOW')
  const [newTransactionAmount, setNewTransactionAmount] = useState<string>('')
  const [newTransactionMemo, setNewTransactionMemo] = useState<string>('')
  const [editingTransactionAccountId, setEditingTransactionAccountId] = useState<string | null>(null)
  const [editingTransactionId, setEditingTransactionId] = useState<string | null>(null)
  const [editingTransactionDate, setEditingTransactionDate] = useState<string>(todayIsoDate())
  const [editingTransactionDirection, setEditingTransactionDirection] =
    useState<TransactionDirection>('OUTFLOW')
  const [editingTransactionAmount, setEditingTransactionAmount] = useState<string>('')
  const [editingTransactionMemo, setEditingTransactionMemo] = useState<string>('')
  const [updateTransactionStatus, setUpdateTransactionStatus] = useState<CreateTransactionStatus>('idle')
  const [updateTransactionErrorMessage, setUpdateTransactionErrorMessage] = useState<string | null>(null)
  const [deletingTransactionId, setDeletingTransactionId] = useState<string | null>(null)
  const [deleteTransactionErrorMessage, setDeleteTransactionErrorMessage] = useState<string | null>(null)
  const [csvFile, setCsvFile] = useState<File | null>(null)
  const csvFileInputRef = useRef<HTMLInputElement | null>(null)

  const resetEditingTransaction = (): void => {
    setEditingTransactionAccountId(null)
    setEditingTransactionId(null)
    setEditingTransactionDate(todayIsoDate())
    setEditingTransactionDirection('OUTFLOW')
    setEditingTransactionAmount('')
    setEditingTransactionMemo('')
    setUpdateTransactionStatus('idle')
    setUpdateTransactionErrorMessage(null)
  }

  const resetDeleteTransactionState = (): void => {
    setDeletingTransactionId(null)
    setDeleteTransactionErrorMessage(null)
  }

  if (status === 'loading' || status === 'idle') {
    return <StatusCard tone="neutral" message="Загружаем инвестиции и балансы..." />
  }

  if (status === 'error') {
    return (
      <StatusCard
        tone="warning"
        message={errorMessage ?? 'Не удалось загрузить инвестиции.'}
        actionLabel="Повторить"
        onAction={onRetryAccounts}
      />
    )
  }

  const isCurrencyValid = ACCOUNT_CURRENCY_OPTIONS.includes(newAccountCurrency)
  const canCreate =
    newAccountName.trim().length > 0 && isCurrencyValid && createAccountStatus !== 'submitting'

  const transactionDateCandidate = newTransactionDate.trim()
  const transactionAmountCandidate = normalizeAmountInput(newTransactionAmount)
  const transactionMemoCandidate = newTransactionMemo.trim()
  const isTransactionDateValid = isValidIsoDateValue(transactionDateCandidate)
  const isTransactionAmountValid = isValidPositiveAmountValue(transactionAmountCandidate)
  const canCreateTransaction =
    selectedAccount !== null &&
    isTransactionDateValid &&
    isTransactionAmountValid &&
    createTransactionStatus !== 'submitting'
  const editingTransactionDateCandidate = editingTransactionDate.trim()
  const editingTransactionAmountCandidate = normalizeAmountInput(editingTransactionAmount)
  const editingTransactionMemoCandidate = editingTransactionMemo.trim()
  const isEditingTransactionDateValid = isValidIsoDateValue(editingTransactionDateCandidate)
  const isEditingTransactionAmountValid = isValidPositiveAmountValue(editingTransactionAmountCandidate)
  const activeEditingTransactionId =
    editingTransactionAccountId === selectedAccountId ? editingTransactionId : null
  const canUpdateTransaction =
    selectedAccount !== null &&
    activeEditingTransactionId !== null &&
    isEditingTransactionDateValid &&
    isEditingTransactionAmountValid &&
    updateTransactionStatus !== 'submitting'
  const canImportCsv =
    selectedAccount !== null &&
    csvFile !== null &&
    csvImportStatus !== 'submitting'
  const isDeleteTransactionSubmitting =
    deletingTransactionId !== null && deleteTransactionErrorMessage === null

  const handleCreateAccountSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!canCreate) {
      return
    }

    const created = await onCreateAccount({
      name: newAccountName.trim(),
      currency: newAccountCurrency,
      type: newAccountType,
    })

    if (created) {
      setNewAccountName('')
    }
  }

  const handleCreateTransactionSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !canCreateTransaction) {
      return
    }

    const created = await onCreateTransaction(selectedAccount.id, {
      occurredOn: transactionDateCandidate,
      direction: newTransactionDirection,
      amount: transactionAmountCandidate,
      memo: transactionMemoCandidate,
    })

    if (created) {
      setNewTransactionAmount('')
      setNewTransactionMemo('')
    }
  }

  const handleImportCsvSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !csvFile || !canImportCsv) {
      return
    }

    const imported = await onImportTransactionsCsv(selectedAccount.id, csvFile)
    if (!imported) {
      return
    }

    setCsvFile(null)
    if (csvFileInputRef.current) {
      csvFileInputRef.current.value = ''
    }
  }

  const handleStartEditingTransaction = (transaction: TransactionDto): void => {
    resetDeleteTransactionState()
    setEditingTransactionAccountId(selectedAccountId)
    setEditingTransactionId(transaction.id)
    setEditingTransactionDate(transaction.occurredOn)
    setEditingTransactionDirection(transaction.direction)
    setEditingTransactionAmount(transaction.amount)
    setEditingTransactionMemo(normalizeTransactionMemo(transaction.memo))
    setUpdateTransactionStatus('idle')
    setUpdateTransactionErrorMessage(null)
  }

  const handleUpdateTransactionSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !activeEditingTransactionId || !canUpdateTransaction) {
      return
    }

    setUpdateTransactionStatus('submitting')
    setUpdateTransactionErrorMessage(null)

    try {
      await onUpdateTransaction(selectedAccount.id, activeEditingTransactionId, {
        occurredOn: editingTransactionDateCandidate,
        direction: editingTransactionDirection,
        amount: editingTransactionAmountCandidate,
        memo: editingTransactionMemoCandidate,
      })
      resetEditingTransaction()
    } catch (error) {
      setUpdateTransactionStatus('error')
      setUpdateTransactionErrorMessage(toErrorMessage(error))
    }
  }

  const handleDeleteTransactionClick = async (transaction: TransactionDto): Promise<void> => {
    if (!selectedAccount || isDeleteTransactionSubmitting) {
      return
    }

    setDeletingTransactionId(transaction.id)
    setDeleteTransactionErrorMessage(null)

    try {
      await onDeleteTransaction(selectedAccount.id, transaction.id)
      if (activeEditingTransactionId === transaction.id) {
        resetEditingTransaction()
      }
      resetDeleteTransactionState()
    } catch (error) {
      setDeletingTransactionId(transaction.id)
      setDeleteTransactionErrorMessage(toErrorMessage(error))
    }
  }

  const handleSelectAccountClick = (accountId: string): void => {
    onSelectAccount(accountId)
    resetEditingTransaction()
    resetDeleteTransactionState()
    setNewTransactionDate(todayIsoDate())
    setNewTransactionDirection('OUTFLOW')
    setNewTransactionAmount('')
    setNewTransactionMemo('')
    setCsvFile(null)
    if (csvFileInputRef.current) {
      csvFileInputRef.current.value = ''
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.3fr)]">
      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <h2 className="text-sm font-semibold text-slate-900">Список инвестиций</h2>
        <p className="mt-1 text-xs text-slate-500">Выберите инструмент, чтобы посмотреть транзакции.</p>

        <form
          className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
          onSubmit={(event) => {
            void handleCreateAccountSubmit(event)
          }}
        >
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Добавить инструмент</p>

          <div className="mt-3 space-y-2">
            <input
              type="text"
              value={newAccountName}
              onChange={(event) => setNewAccountName(event.target.value)}
              placeholder="Название инструмента"
              className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
            />

            <div className="grid gap-2 sm:grid-cols-2">
              <select
                value={newAccountCurrency}
                onChange={(event) => setNewAccountCurrency(event.target.value)}
                className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
              >
                {ACCOUNT_CURRENCY_OPTIONS.map((currencyCode) => (
                  <option key={currencyCode} value={currencyCode}>
                    {currencyCode}
                  </option>
                ))}
              </select>

              <select
                value={newAccountType}
                onChange={(event) => setNewAccountType(event.target.value as AccountType)}
                className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
              >
                {ACCOUNT_TYPE_OPTIONS.map((type) => (
                  <option key={type} value={type}>
                    {toAccountTypeLabel(type)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {!isCurrencyValid ? (
            <p className="mt-2 text-xs text-amber-700">Выберите валюту из списка.</p>
          ) : null}

          {createAccountStatus === 'error' && createAccountErrorMessage ? (
            <p className="mt-2 text-xs text-amber-700">{createAccountErrorMessage}</p>
          ) : null}

          <button
            type="submit"
            disabled={!canCreate}
            className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {createAccountStatus === 'submitting' ? 'Добавляем...' : 'Добавить'}
          </button>
        </form>

        {accounts.length === 0 ? (
          <div className="mt-4">
            <StatusCard tone="neutral" message="Пока нет инвестиций. Добавьте первый инструмент." />
          </div>
        ) : (
          <ul className="mt-4 space-y-2">
            {accounts.map((account) => (
              <li key={account.id}>
                <button
                  type="button"
                  onClick={() => handleSelectAccountClick(account.id)}
                  className={`w-full rounded-lg border px-3 py-3 text-left ${
                    selectedAccountId === account.id
                      ? 'border-slate-300 bg-white shadow-sm'
                      : 'border-slate-200 bg-transparent'
                  }`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{account.name}</p>
                      <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                        {toAccountTypeLabel(account.type)} · {toAccountStatusLabel(account.status)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs uppercase tracking-wide text-slate-500">
                        {account.balance.currency}
                      </p>
                      <p className="mt-1 text-sm font-semibold text-slate-900">
                        {formatAmount(account.balance.amount)}
                      </p>
                    </div>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </article>

      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        {!selectedAccount ? (
          <StatusCard tone="neutral" message="Выберите инструмент, чтобы посмотреть транзакции." />
        ) : (
          <>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">{selectedAccount.name}</h2>
                <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                  Транзакции · {selectedAccount.balance.currency}
                </p>
              </div>
            </div>

            <form
              className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
              onSubmit={(event) => {
                void handleCreateTransactionSubmit(event)
              }}
            >
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Добавить транзакцию
              </p>

              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                <label className="text-xs text-slate-600">
                  Дата
                  <input
                    type="date"
                    value={newTransactionDate}
                    onChange={(event) => setNewTransactionDate(event.target.value)}
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>

                <label className="text-xs text-slate-600">
                  Направление
                  <select
                    value={newTransactionDirection}
                    onChange={(event) =>
                      setNewTransactionDirection(event.target.value as TransactionDirection)
                    }
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  >
                    <option value="OUTFLOW">{toDirectionSelectLabel('OUTFLOW')}</option>
                    <option value="INFLOW">{toDirectionSelectLabel('INFLOW')}</option>
                  </select>
                </label>
              </div>

              <div className="mt-2 grid gap-2 sm:grid-cols-[minmax(0,0.6fr)_minmax(0,1fr)]">
                <label className="text-xs text-slate-600">
                  Сумма
                  <input
                    type="text"
                    inputMode="decimal"
                    value={newTransactionAmount}
                    onChange={(event) => setNewTransactionAmount(event.target.value)}
                    placeholder="0,00"
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>

                <label className="text-xs text-slate-600">
                  Описание
                  <input
                    type="text"
                    value={newTransactionMemo}
                    onChange={(event) => setNewTransactionMemo(event.target.value)}
                    placeholder="Продукты"
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>
              </div>

              {!isTransactionAmountValid && newTransactionAmount.trim().length > 0 ? (
                <p className="mt-2 text-xs text-amber-700">
                  Сумма должна быть положительной и содержать не более 2 знаков после запятой.
                </p>
              ) : null}

              {createTransactionStatus === 'error' && createTransactionErrorMessage ? (
                <p className="mt-2 text-xs text-amber-700">{createTransactionErrorMessage}</p>
              ) : null}

              <button
                type="submit"
                disabled={!canCreateTransaction}
                className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {createTransactionStatus === 'submitting' ? 'Добавляем...' : 'Добавить транзакцию'}
              </button>
            </form>

            <form
              className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
              onSubmit={(event) => {
                void handleImportCsvSubmit(event)
              }}
            >
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Импорт из CSV</p>
              <p className="mt-1 text-xs text-slate-500">
                Формат заголовка: <code>occurred_on,direction,amount,currency,memo</code>
              </p>

              <label className="mt-3 block text-xs text-slate-600">
                Файл
                <input
                  ref={csvFileInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  onChange={(event) => setCsvFile(event.target.files?.[0] ?? null)}
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-2 file:py-1 file:text-xs file:font-medium file:text-slate-700"
                />
              </label>

              {csvFile ? (
                <p className="mt-2 text-xs text-slate-500">
                  Выбран файл: <span className="font-medium text-slate-700">{csvFile.name}</span>
                </p>
              ) : null}

              {csvImportStatus === 'error' && csvImportErrorMessage ? (
                <p className="mt-2 text-xs text-amber-700">{csvImportErrorMessage}</p>
              ) : null}

              {csvImportStatus === 'success' && csvImportResult ? (
                <p className="mt-2 text-xs text-emerald-700">
                  Импорт завершен: получено {csvImportResult.receivedRows}, добавлено{' '}
                  {csvImportResult.importedCount}, пропущено дубликатов{' '}
                  {csvImportResult.skippedDuplicates}.
                </p>
              ) : null}

              <button
                type="submit"
                disabled={!canImportCsv}
                className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {csvImportStatus === 'submitting' ? 'Импортируем...' : 'Импортировать CSV'}
              </button>
            </form>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <label className="text-xs text-slate-600">
                Направление
                <select
                  value={directionFilter}
                  onChange={(event) =>
                    onDirectionFilterChange(event.target.value as TransactionDirectionFilter)
                  }
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                >
                  <option value="ALL">Все</option>
                  <option value="INFLOW">Доходы</option>
                  <option value="OUTFLOW">Расходы</option>
                </select>
              </label>

              <label className="text-xs text-slate-600">
                Фильтр по описанию
                <input
                  type="text"
                  value={memoFilter}
                  onChange={(event) => onMemoFilterChange(event.target.value)}
                  placeholder="Поиск по описанию"
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                />
              </label>
            </div>

            <div className="mt-4">
              {transactionsStatus === 'loading' || transactionsStatus === 'idle' ? (
                <StatusCard tone="neutral" message="Загружаем транзакции..." />
              ) : null}

              {transactionsStatus === 'error' ? (
                <StatusCard
                  tone="warning"
                  message={transactionsErrorMessage ?? 'Не удалось загрузить транзакции.'}
                  actionLabel="Повторить"
                  onAction={onRetryTransactions}
                />
              ) : null}

              {transactionsStatus === 'ready' && totalTransactionsCount === 0 ? (
                <StatusCard tone="neutral" message="Для этого инструмента пока нет транзакций." />
              ) : null}

              {transactionsStatus === 'ready' && totalTransactionsCount > 0 && filteredTransactions.length === 0 ? (
                <StatusCard tone="neutral" message="Нет транзакций, подходящих под фильтры." />
              ) : null}

              {transactionsStatus === 'ready' && filteredTransactions.length > 0 ? (
                <ul className="space-y-2">
                  {filteredTransactions.map((transaction) => {
                    const isEditing = activeEditingTransactionId === transaction.id
                    const hasDeleteError =
                      deletingTransactionId === transaction.id && deleteTransactionErrorMessage !== null
                    const isDeletingThisTransaction =
                      deletingTransactionId === transaction.id && deleteTransactionErrorMessage === null

                    return (
                      <li
                        key={transaction.id}
                        className="rounded-lg border border-slate-200 bg-white px-3 py-3"
                      >
                        {isEditing ? (
                          <form
                            onSubmit={(event) => {
                              void handleUpdateTransactionSubmit(event)
                            }}
                          >
                            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                              Редактировать транзакцию
                            </p>

                            <div className="mt-3 grid gap-2 sm:grid-cols-2">
                              <label className="text-xs text-slate-600">
                                Дата
                                <input
                                  type="date"
                                  value={editingTransactionDate}
                                  onChange={(event) => setEditingTransactionDate(event.target.value)}
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>

                              <label className="text-xs text-slate-600">
                                Направление
                                <select
                                  value={editingTransactionDirection}
                                  onChange={(event) =>
                                    setEditingTransactionDirection(
                                      event.target.value as TransactionDirection,
                                    )
                                  }
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                >
                                  <option value="OUTFLOW">{toDirectionSelectLabel('OUTFLOW')}</option>
                                  <option value="INFLOW">{toDirectionSelectLabel('INFLOW')}</option>
                                </select>
                              </label>
                            </div>

                            <div className="mt-2 grid gap-2 sm:grid-cols-[minmax(0,0.6fr)_minmax(0,1fr)]">
                              <label className="text-xs text-slate-600">
                                Сумма
                                <input
                                  type="text"
                                  inputMode="decimal"
                                  value={editingTransactionAmount}
                                  onChange={(event) => setEditingTransactionAmount(event.target.value)}
                                  placeholder="0,00"
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>

                              <label className="text-xs text-slate-600">
                                Описание
                                <input
                                  type="text"
                                  value={editingTransactionMemo}
                                  onChange={(event) => setEditingTransactionMemo(event.target.value)}
                                  placeholder="Без описания"
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>
                            </div>

                            {!isEditingTransactionAmountValid &&
                            editingTransactionAmount.trim().length > 0 ? (
                              <p className="mt-2 text-xs text-amber-700">
                                Сумма должна быть положительной и содержать не более 2 знаков после
                                запятой.
                              </p>
                            ) : null}

                            {updateTransactionStatus === 'error' && updateTransactionErrorMessage ? (
                              <p className="mt-2 text-xs text-amber-700">
                                {updateTransactionErrorMessage}
                              </p>
                            ) : null}

                            <div className="mt-3 flex flex-wrap gap-2">
                              <button
                                type="submit"
                                disabled={!canUpdateTransaction}
                                className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                {updateTransactionStatus === 'submitting'
                                  ? 'Сохраняем...'
                                  : 'Сохранить'}
                              </button>
                              <button
                                type="button"
                                onClick={resetEditingTransaction}
                                className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600"
                              >
                                Отмена
                              </button>
                            </div>
                          </form>
                        ) : (
                          <div className="flex items-center justify-between gap-3">
                            <div>
                              <p className="text-sm font-medium text-slate-900">
                                {toTransactionMemoDisplay(transaction.memo)}
                              </p>
                              <p className="mt-1 text-xs text-slate-500">
                                {formatIsoDateRu(transaction.occurredOn)} ·{' '}
                                {toDirectionLabel(transaction.direction)}
                              </p>
                            </div>
                            <div className="text-right">
                              <p className="text-xs uppercase tracking-wide text-slate-500">
                                {transaction.currency}
                              </p>
                              <p
                                className={`mt-1 text-sm font-semibold ${
                                  transaction.direction === 'OUTFLOW'
                                    ? 'text-rose-700'
                                    : 'text-emerald-700'
                                }`}
                              >
                                {formatSignedAmount(transaction.amount, transaction.direction)}
                              </p>
                              <div className="mt-2 flex flex-wrap justify-end gap-2">
                                <button
                                  type="button"
                                  onClick={() => handleStartEditingTransaction(transaction)}
                                  disabled={isDeleteTransactionSubmitting}
                                  className="rounded-md border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  Редактировать
                                </button>
                                <button
                                  type="button"
                                  onClick={() => {
                                    void handleDeleteTransactionClick(transaction)
                                  }}
                                  disabled={isDeleteTransactionSubmitting}
                                  className="rounded-md border border-rose-200 px-2.5 py-1 text-xs font-medium text-rose-700 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  {isDeletingThisTransaction ? 'Удаляем...' : 'Удалить'}
                                </button>
                              </div>
                              {hasDeleteError ? (
                                <p className="mt-2 text-xs text-amber-700">{deleteTransactionErrorMessage}</p>
                              ) : null}
                            </div>
                          </div>
                        )}
                      </li>
                    )
                  })}
                </ul>
              ) : null}
            </div>
          </>
        )}
      </article>
    </div>
  )
}

interface TabButtonProps {
  label: string
  isActive: boolean
  onClick: () => void
}

function TabButton({ label, isActive, onClick }: TabButtonProps) {
  const activeClasses = isActive
    ? 'bg-white text-slate-900 shadow-sm'
    : 'bg-transparent text-slate-500 hover:text-slate-800'

  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${activeClasses}`}
    >
      {label}
    </button>
  )
}

interface MetricCardProps {
  title: string
  subtitle: string
  totals: CurrencyTotalsDto
}

function MetricCard({ title, subtitle, totals }: MetricCardProps) {
  const entries = Object.entries(totals).sort(([left], [right]) => left.localeCompare(right))

  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
      <p className="mt-1 text-xs leading-relaxed text-slate-500">{subtitle}</p>

      {entries.length === 0 ? (
        <p className="mt-6 text-sm text-slate-600">Пока нет данных.</p>
      ) : (
        <ul className="mt-6 space-y-2">
          {entries.map(([currency, amount]) => (
            <li key={currency} className="flex items-center justify-between gap-3 text-sm">
              <span className="font-medium text-slate-700">{currency}</span>
              <span className="font-semibold text-slate-900">{formatAmount(amount)}</span>
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}

interface StatusCardProps {
  tone: 'neutral' | 'warning'
  message: string
  actionLabel?: string
  onAction?: () => void
}

function StatusCard({ tone, message, actionLabel, onAction }: StatusCardProps) {
  const toneClasses =
    tone === 'warning'
      ? 'border-amber-200 bg-amber-50 text-amber-900'
      : 'border-slate-200 bg-slate-50 text-slate-600'

  return (
    <div className={`rounded-xl border px-4 py-5 text-sm ${toneClasses}`}>
      <p>{message}</p>
      {actionLabel && onAction ? (
        <button
          type="button"
          onClick={onAction}
          className="mt-3 rounded-md border border-current px-3 py-1 text-xs font-medium"
        >
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}

function formatAmount(amount: string): string {
  const isNegative = amount.startsWith('-')
  const unsigned = isNegative ? amount.slice(1) : amount
  const [integerRaw, fractionRaw = '00'] = unsigned.split('.')
  const integerPart = integerRaw.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  const fractionPart = fractionRaw.padEnd(2, '0').slice(0, 2)
  return `${isNegative ? '-' : ''}${integerPart},${fractionPart}`
}

function formatSignedAmount(amount: string, direction: 'INFLOW' | 'OUTFLOW'): string {
  const prefix = direction === 'OUTFLOW' ? '-' : '+'
  return `${prefix}${formatAmount(amount)}`
}

function normalizeAmountInput(value: string): string {
  return value.trim().replace(',', '.')
}

function isValidIsoDateValue(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(value)
}

function isValidPositiveAmountValue(value: string): boolean {
  return /^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(value) && Number(value) > 0
}

function normalizeTransactionMemo(memo: string | null): string {
  return memo ?? ''
}

function toTransactionMemoDisplay(memo: string | null): string {
  const normalizedMemo = normalizeTransactionMemo(memo)
  return normalizedMemo.length > 0 ? normalizedMemo : 'Без описания'
}

function todayIsoDate(): string {
  const now = new Date()
  const year = String(now.getFullYear())
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatIsoDateRu(isoDate: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) {
    return isoDate
  }

  const [year, month, day] = isoDate.split('-')
  return `${day}.${month}.${year}`
}

function toDirectionLabel(direction: TransactionDirection): string {
  return toDirectionSelectLabel(direction)
}

function toDirectionSelectLabel(direction: TransactionDirection): string {
  return direction === 'INFLOW' ? 'Доход' : 'Расход'
}

function toAccountTypeLabel(type: AccountType): string {
  const labels: Record<AccountType, string> = {
    CASH: 'Наличные',
    DEPOSIT: 'Депозит',
    FUND: 'Фонд',
    IIS: 'ИИС',
    BROKERAGE: 'Брокерский',
  }
  return labels[type]
}

function toAccountStatusLabel(status: AccountStatus): string {
  if (status === 'ACTIVE') {
    return 'Активный'
  }
  if (status === 'ARCHIVED') {
    return 'Архивный'
  }
  return status
}

function enrichPersonalFinanceCards(
  cards: PersonalFinanceCardDto[],
  summaryByCardId: Map<
    string,
    {
      currentBalance: string | null
      currency: string | null
    }
  > = new Map(),
): PersonalFinanceCardListItem[] {
  return cards.map((card) => {
    const summary = summaryByCardId.get(card.id)

    return {
      ...card,
      currentBalance: summary?.currentBalance ?? null,
      currency: summary?.currency ?? null,
    }
  })
}

function resolvePersonalFinanceSelection(
  cards: PersonalFinanceCardDto[],
  selectedCardId: string | null,
): string | null {
  if (selectedCardId && cards.some((card) => card.id === selectedCardId)) {
    return selectedCardId
  }

  return cards.find((card) => card.status === 'ACTIVE')?.id ?? cards[0]?.id ?? null
}

function buildPersonalFinanceCardSummary(
  activeSnapshots: PersonalFinanceSnapshotDto[],
  settingsSnapshot: PersonalFinanceSnapshotDto | null,
): Map<
  string,
  {
    currentBalance: string | null
    currency: string | null
  }
> {
  const summaryByCardId = new Map<
    string,
    {
      currentBalance: string | null
      currency: string | null
    }
  >()

  activeSnapshots.forEach((snapshot) => {
    summaryByCardId.set(snapshot.card.id, {
      currentBalance: snapshot.settings.currentBalance,
      currency: snapshot.currency,
    })
  })

  if (settingsSnapshot && !summaryByCardId.has(settingsSnapshot.card.id)) {
    summaryByCardId.set(settingsSnapshot.card.id, {
      currentBalance: settingsSnapshot.settings.currentBalance,
      currency: settingsSnapshot.currency,
    })
  }

  return summaryByCardId
}

function resolvePersonalFinanceSettingsSelection(
  cards: PersonalFinanceCardDto[],
  resolvedCardId: string | null,
  activeSnapshots: PersonalFinanceSnapshotDto[],
): string | null {
  const resolvedCard = cards.find((card) => card.id === resolvedCardId) ?? null
  if (!resolvedCard) {
    return activeSnapshots[0]?.card.id ?? cards[0]?.id ?? null
  }

  if (resolvedCard.status === 'ARCHIVED') {
    return resolvedCard.id
  }

  if (activeSnapshots.some((snapshot) => snapshot.card.id === resolvedCard.id)) {
    return resolvedCard.id
  }

  return activeSnapshots[0]?.card.id ?? resolvedCard.id
}

function getAccountCurrencyOptions(): string[] {
  const fallbackOptions = ['USD', 'EUR', 'RUB', 'GBP', 'CHF', 'CNY', 'JPY', 'KZT', 'TRY']
  const intlWithSupportedValues = Intl as typeof Intl & {
    supportedValuesOf?: (key: 'calendar' | 'collation' | 'currency' | 'numberingSystem' | 'timeZone' | 'unit') => string[]
  }
  const dynamicOptions = intlWithSupportedValues.supportedValuesOf?.('currency')
  if (!dynamicOptions || dynamicOptions.length === 0) {
    return fallbackOptions
  }

  return [...new Set(dynamicOptions.map((currencyCode) => currencyCode.toUpperCase()))].sort((left, right) =>
    left.localeCompare(right),
  )
}

function getViewTitle(view: ViewTab): string {
  if (view === 'accounts') {
    return 'Инвестиции'
  }
  if (view === 'personal-finance') {
    return 'Личные финансы'
  }
  return 'Обзор'
}

function getViewDescription(view: ViewTab): string {
  if (view === 'accounts') {
    return 'Инвестиционные счета и активы с балансами, деталями транзакций и простыми фильтрами.'
  }
  if (view === 'personal-finance') {
    return 'Ручной yearly review по картам: факт расходов, лимиты, фактический доход и прогноз.'
  }
  return 'Спокойный срез капитала и метрик финансового спокойствия.'
}

function readNavigationFromUrl(): NavigationState {
  if (typeof window === 'undefined') {
    return DEFAULT_NAVIGATION_STATE
  }

  const params = new URLSearchParams(window.location.search)
  const tabParam = params.get('tab')
  const accountIdRaw = params.get('accountId')
  const financeTabParam = params.get('financeTab')
  const financeYearParam = params.get('financeYear')
  const financeCardIdRaw = params.get('financeCardId')
  const accountId = accountIdRaw && accountIdRaw.trim().length > 0 ? accountIdRaw : null
  const financeCardId = financeCardIdRaw && financeCardIdRaw.trim().length > 0 ? financeCardIdRaw : null
  const financeTab: PersonalFinanceTab =
    financeTabParam === 'settings'
      ? 'settings'
      : financeTabParam === 'income'
        ? 'income'
        : 'expenses'
  const financeYear = toNavigationYear(financeYearParam)

  const tab: ViewTab =
    tabParam === 'personal-finance'
      ? 'personal-finance'
      : tabParam === 'accounts' || accountId
        ? 'accounts'
        : 'dashboard'

  return { tab, accountId, financeTab, financeYear, financeCardId }
}

function syncNavigationToUrl(
  activeView: ViewTab,
  selectedAccountId: string | null,
  financeTab: PersonalFinanceTab,
  financeYear: number,
  financeCardId: string | null,
): void {
  if (typeof window === 'undefined') {
    return
  }

  const params = new URLSearchParams(window.location.search)
  params.delete('tab')
  params.delete('accountId')
  params.delete('financeTab')
  params.delete('financeYear')
  params.delete('financeCardId')

  params.set('tab', activeView)
  if (activeView === 'accounts' && selectedAccountId) {
    params.set('accountId', selectedAccountId)
  }
  if (activeView === 'personal-finance') {
    params.set('financeTab', financeTab)
    params.set('financeYear', String(financeYear))
    if (financeCardId) {
      params.set('financeCardId', financeCardId)
    }
  }

  const query = params.toString()
  const nextUrl = `${window.location.pathname}${query ? `?${query}` : ''}`
  window.history.replaceState(null, '', nextUrl)
}

function toNavigationYear(value: string | null): number {
  if (!value) {
    return currentYear()
  }

  const parsed = Number.parseInt(value, 10)
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 9999) {
    return currentYear()
  }

  return parsed
}

function currentYear(): number {
  return new Date().getFullYear()
}

function toErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return `${error.code ?? 'REQUEST_ERROR'} (${error.status}): ${error.message}`
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Непредвиденная ошибка при загрузке данных.'
}

export default App
