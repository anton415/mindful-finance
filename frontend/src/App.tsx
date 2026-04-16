import { startTransition, useEffect, useMemo, useState } from 'react'
import {
  ApiClientError,
  apiClient,
  type CreatePersonalFinanceCardRequest,
  type CreatePersonalFinanceTransferRequest,
  type CreateAccountRequest,
  type CreateTransactionRequest,
  type CurrencyTotalsDto,
  type ImportTransactionsCsvResponse,
  type PersonalFinanceCardDto,
  type PersonalFinanceSnapshotDto,
  type TransactionDto,
  type UpdateIncomePlanRequest,
  type UpdateAccountRequest,
  type UpdateMonthlyExpenseRequest,
  type UpdateMonthlyIncomeActualRequest,
  type UpdatePersonalFinanceCardRequest,
  type UpdatePersonalFinanceSettingsRequest,
  type UpdateTransactionRequest,
} from './api'
import {
  InvestmentsView,
  formatIsoDateRu,
  normalizeTransactionMemo,
  todayIsoDate,
  MetricCard,
  StatusCard,
  type AccountWithBalance,
  type CreateAccountStatus,
  type CreateTransactionStatus,
  type CsvImportStatus,
  type LoadStatus,
  type TransactionDirectionFilter,
} from './features/investments'
import {
  PersonalFinanceView,
  type PersonalFinanceCardListItem,
  type PersonalFinanceTab,
} from './features/personal-finance'
import { AppShell, type ViewTab } from './features/app-shell'

interface DashboardData {
  asOf: string
  netWorth: CurrencyTotalsDto
  monthlyBurn: CurrencyTotalsDto
  monthlySavings: CurrencyTotalsDto
}

interface NavigationState {
  tab: ViewTab
  accountId: string | null
  financeTab: PersonalFinanceTab
  financeYear: number
  financeCardId: string | null
}

const DEFAULT_NAVIGATION_STATE: NavigationState = {
  tab: 'dashboard',
  accountId: null,
  financeTab: 'expenses',
  financeYear: currentYear(),
  financeCardId: null,
}

function App() {
  const [activeView, setActiveView] = useState<ViewTab>(
    () => readNavigationFromUrl().tab,
  )
  const [activePersonalFinanceTab, setActivePersonalFinanceTab] =
    useState<PersonalFinanceTab>(() => readNavigationFromUrl().financeTab)
  const [selectedPersonalFinanceYear, setSelectedPersonalFinanceYear] =
    useState<number>(() => readNavigationFromUrl().financeYear)
  const [selectedPersonalFinanceCardId, setSelectedPersonalFinanceCardId] =
    useState<string | null>(() => readNavigationFromUrl().financeCardId)

  const [dashboardStatus, setDashboardStatus] = useState<LoadStatus>('idle')
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [dashboardErrorMessage, setDashboardErrorMessage] = useState<
    string | null
  >(null)
  const [dashboardReloadTick, setDashboardReloadTick] = useState<number>(0)

  const [accountsStatus, setAccountsStatus] = useState<LoadStatus>('idle')
  const [accounts, setAccounts] = useState<AccountWithBalance[]>([])
  const [accountsErrorMessage, setAccountsErrorMessage] = useState<
    string | null
  >(null)
  const [accountsReloadTick, setAccountsReloadTick] = useState<number>(0)
  const [createAccountStatus, setCreateAccountStatus] =
    useState<CreateAccountStatus>('idle')
  const [createAccountErrorMessage, setCreateAccountErrorMessage] = useState<
    string | null
  >(null)

  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(
    () => readNavigationFromUrl().accountId,
  )
  const [transactionsStatus, setTransactionsStatus] =
    useState<LoadStatus>('idle')
  const [transactions, setTransactions] = useState<TransactionDto[]>([])
  const [transactionsErrorMessage, setTransactionsErrorMessage] = useState<
    string | null
  >(null)
  const [transactionsReloadTick, setTransactionsReloadTick] =
    useState<number>(0)
  const [createTransactionStatus, setCreateTransactionStatus] =
    useState<CreateTransactionStatus>('idle')
  const [createTransactionErrorMessage, setCreateTransactionErrorMessage] =
    useState<string | null>(null)
  const [csvImportStatus, setCsvImportStatus] =
    useState<CsvImportStatus>('idle')
  const [csvImportErrorMessage, setCsvImportErrorMessage] = useState<
    string | null
  >(null)
  const [csvImportResult, setCsvImportResult] =
    useState<ImportTransactionsCsvResponse | null>(null)
  const [personalFinanceStatus, setPersonalFinanceStatus] =
    useState<LoadStatus>('idle')
  const [personalFinanceCards, setPersonalFinanceCards] = useState<
    PersonalFinanceCardListItem[]
  >([])
  const [activePersonalFinanceSnapshots, setActivePersonalFinanceSnapshots] =
    useState<PersonalFinanceSnapshotDto[]>([])
  const [
    selectedPersonalFinanceSettingsSnapshot,
    setSelectedPersonalFinanceSettingsSnapshot,
  ] = useState<PersonalFinanceSnapshotDto | null>(null)
  const [personalFinanceErrorMessage, setPersonalFinanceErrorMessage] =
    useState<string | null>(null)
  const [personalFinanceReloadTick, setPersonalFinanceReloadTick] =
    useState<number>(0)

  const [directionFilter, setDirectionFilter] =
    useState<TransactionDirectionFilter>('ALL')
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
          accountRows.map((account) =>
            apiClient.getAccountBalance(account.id, controller.signal),
          ),
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

          if (
            currentSelection &&
            rows.some((account) => account.id === currentSelection)
          ) {
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
        const rows = await apiClient.listAccountTransactions(
          selectedAccountId,
          controller.signal,
        )

        if (controller.signal.aborted) {
          return
        }

        const sortedRows = [...rows].sort((left, right) =>
          right.occurredOn.localeCompare(left.occurredOn),
        )
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
        const cards = await apiClient.listPersonalFinanceCards(
          controller.signal,
        )
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

        const resolvedCardId = resolvePersonalFinanceSelection(
          cards,
          selectedPersonalFinanceCardId,
        )
        if (resolvedCardId !== selectedPersonalFinanceCardId) {
          setSelectedPersonalFinanceCardId(resolvedCardId)
        }

        const activeSnapshotResults = await Promise.allSettled(
          activeCards.map((card) =>
            apiClient.getPersonalFinanceSnapshot(
              card.id,
              selectedPersonalFinanceYear,
              controller.signal,
            ),
          ),
        )

        if (controller.signal.aborted) {
          return
        }

        const activeSnapshots = activeSnapshotResults.flatMap((result) =>
          result.status === 'fulfilled' ? [result.value] : [],
        )
        const normalizedActiveSnapshots = activeSnapshots.map(
          normalizePersonalFinanceSnapshotCategoryOrder,
        )
        const settingsCardId = resolvePersonalFinanceSettingsSelection(
          cards,
          resolvedCardId,
          normalizedActiveSnapshots,
        )

        if (settingsCardId !== selectedPersonalFinanceCardId) {
          setSelectedPersonalFinanceCardId(settingsCardId)
        }

        let settingsSnapshot =
          normalizedActiveSnapshots.find(
            (snapshot) => snapshot.card.id === settingsCardId,
          ) ?? null

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
            settingsSnapshot = normalizePersonalFinanceSnapshotCategoryOrder(
              fulfilledSettingsSnapshot.value,
            )
          }
        }

        if (controller.signal.aborted) {
          return
        }

        const mergedActiveSnapshots =
          settingsSnapshot &&
          settingsSnapshot.card.status === 'ACTIVE' &&
          !normalizedActiveSnapshots.some(
            (snapshot) => snapshot.card.id === settingsSnapshot.card.id,
          )
            ? [...normalizedActiveSnapshots, settingsSnapshot]
            : normalizedActiveSnapshots

        setActivePersonalFinanceSnapshots(mergedActiveSnapshots)
        setSelectedPersonalFinanceSettingsSnapshot(settingsSnapshot)
        setPersonalFinanceCards(
          enrichPersonalFinanceCards(
            cards,
            buildPersonalFinanceCardSummary(
              mergedActiveSnapshots,
              settingsSnapshot,
            ),
          ),
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
  }, [
    activeView,
    selectedPersonalFinanceCardId,
    selectedPersonalFinanceYear,
    personalFinanceReloadTick,
  ])

  const selectedAccount = selectedAccountId
    ? (accounts.find((account) => account.id === selectedAccountId) ?? null)
    : null

  const filteredTransactions = useMemo(() => {
    const normalizedMemoFilter = memoFilter.trim().toLowerCase()

    return transactions.filter((transaction) => {
      if (
        directionFilter !== 'ALL' &&
        transaction.direction !== directionFilter
      ) {
        return false
      }

      if (normalizedMemoFilter.length > 0) {
        return normalizeTransactionMemo(transaction.memo)
          .toLowerCase()
          .includes(normalizedMemoFilter)
      }

      return true
    })
  }, [transactions, directionFilter, memoFilter])

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

  const handleCreateAccount = async (
    request: CreateAccountRequest,
  ): Promise<boolean> => {
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

  const handleUpdateAccount = async (
    accountId: string,
    request: UpdateAccountRequest,
  ): Promise<void> => {
    await apiClient.updateAccount(accountId, request)
    setAccountsReloadTick((tick) => tick + 1)
  }

  const handleDeleteAccount = async (accountId: string): Promise<void> => {
    await apiClient.deleteAccount(accountId)

    if (selectedAccountId === accountId) {
      setSelectedAccountId(null)
      setTransactions([])
      setTransactionsStatus('idle')
      setTransactionsErrorMessage(null)
      setCreateTransactionStatus('idle')
      setCreateTransactionErrorMessage(null)
      setCsvImportStatus('idle')
      setCsvImportErrorMessage(null)
      setCsvImportResult(null)
      setDirectionFilter('ALL')
      setMemoFilter('')
    }

    setAccounts((current) =>
      current.filter((account) => account.id !== accountId),
    )
    setAccountsReloadTick((tick) => tick + 1)
    setDashboardReloadTick((tick) => tick + 1)
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

  const handleImportTransactionsCsv = async (
    accountId: string,
    file: File,
  ): Promise<boolean> => {
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
    setTransactions((current) =>
      current.filter((transaction) => transaction.id !== transactionId),
    )
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

  const handleCreatePersonalFinanceTransfer = async (
    request: CreatePersonalFinanceTransferRequest,
  ): Promise<boolean> => {
    try {
      await apiClient.createPersonalFinanceTransfer(request)
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
      await apiClient.updateMonthlyIncomeActual(cardId, month, request)
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleSaveIncomePlan = async (
    cardId: string,
    year: number,
    request: UpdateIncomePlanRequest,
  ): Promise<boolean> => {
    if (cardId.trim().length === 0) {
      return false
    }

    try {
      await apiClient.updateIncomePlan(cardId, year, request)
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
      await apiClient.updatePersonalFinanceSettings(
        selectedPersonalFinanceCardId,
        request,
      )
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
      await apiClient.updatePersonalFinanceCard(
        selectedPersonalFinanceCardId,
        request,
      )
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  const handleArchivePersonalFinanceCard = async (
    cardId: string,
  ): Promise<boolean> => {
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

  const handleRestorePersonalFinanceCard = async (
    cardId: string,
  ): Promise<boolean> => {
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

  const handleDeletePersonalFinanceCard = async (
    cardId: string,
  ): Promise<boolean> => {
    try {
      await apiClient.deletePersonalFinanceCard(cardId)
      setSelectedPersonalFinanceCardId((current) =>
        current === cardId ? null : current,
      )
      setActivePersonalFinanceTab('settings')
      refreshPersonalFinanceDerivedViews()
      return true
    } catch {
      return false
    }
  }

  return (
    <AppShell
      activeView={activeView}
      headerContextLabel={headerContextLabel}
      onSelectView={setActiveView}
    >
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
          onCreateTransfer={handleCreatePersonalFinanceTransfer}
          onSaveIncomeActual={handleSaveIncomeActual}
          onSaveIncomePlan={handleSaveIncomePlan}
          onRenameCard={handleRenamePersonalFinanceCard}
          onSaveSettings={handleSavePersonalFinanceSettings}
          onArchiveCard={handleArchivePersonalFinanceCard}
          onRestoreCard={handleRestorePersonalFinanceCard}
          onDeleteCard={handleDeletePersonalFinanceCard}
        />
      ) : (
        <InvestmentsView
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
          onUpdateAccount={handleUpdateAccount}
          onDeleteAccount={handleDeleteAccount}
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
    </AppShell>
  )
}

interface DashboardViewProps {
  status: LoadStatus
  dashboard: DashboardData | null
  errorMessage: string | null
  onRetry: () => void
}

function DashboardView({
  status,
  dashboard,
  errorMessage,
  onRetry,
}: DashboardViewProps) {
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
        Общие метрики учитывают и инвестиционные счета, и активные карты из
        раздела личных финансов.
      </p>

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard
          title="Капитал"
          subtitle="С группировкой по валютам"
          totals={dashboard.netWorth}
        />
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

function normalizePersonalFinanceSnapshotCategoryOrder(
  snapshot: PersonalFinanceSnapshotDto,
): PersonalFinanceSnapshotDto {
  const categories = swapEducationAndInvestments(snapshot.categories)
  if (categories === snapshot.categories) {
    return snapshot
  }

  return {
    ...snapshot,
    categories,
  }
}

function swapEducationAndInvestments(
  categories: PersonalFinanceSnapshotDto['categories'],
): PersonalFinanceSnapshotDto['categories'] {
  const investmentsIndex = categories.findIndex(
    (category) => category.code === 'INVESTMENTS',
  )
  const educationIndex = categories.findIndex(
    (category) => category.code === 'EDUCATION',
  )

  if (
    investmentsIndex < 0 ||
    educationIndex < 0 ||
    investmentsIndex === educationIndex
  ) {
    return categories
  }

  const reorderedCategories = [...categories]
  ;[
    reorderedCategories[investmentsIndex],
    reorderedCategories[educationIndex],
  ] = [
    reorderedCategories[educationIndex],
    reorderedCategories[investmentsIndex],
  ]
  return reorderedCategories
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

  return (
    cards.find((card) => card.status === 'ACTIVE')?.id ?? cards[0]?.id ?? null
  )
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

  if (
    activeSnapshots.some((snapshot) => snapshot.card.id === resolvedCard.id)
  ) {
    return resolvedCard.id
  }

  return activeSnapshots[0]?.card.id ?? resolvedCard.id
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
  const accountId =
    accountIdRaw && accountIdRaw.trim().length > 0 ? accountIdRaw : null
  const financeCardId =
    financeCardIdRaw && financeCardIdRaw.trim().length > 0
      ? financeCardIdRaw
      : null
  const financeTab: PersonalFinanceTab =
    financeTabParam === 'settings'
      ? 'settings'
      : financeTabParam === 'income-entry'
        ? 'income-entry'
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
