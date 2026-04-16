import { useRef, useState, type FormEvent } from 'react'
import {
  ApiClientError,
  type AccountType,
  type CreateAccountRequest,
  type CreateTransactionRequest,
  type ImportTransactionsCsvResponse,
  type TransactionDirection,
  type TransactionDto,
  type UpdateAccountRequest,
  type UpdateTransactionRequest,
} from '../../api'
import { formatMoneyInput, normalizeMoneyInput } from '../../money-input'
import {
  isValidIsoDateValue,
  isValidPositiveAmountValue,
  normalizeTransactionMemo,
  toInvestmentAccountCountLabel,
  todayIsoDate,
} from './formatting'
import { InvestmentOverviewTab } from './InvestmentOverviewTab'
import { InvestmentSettingsTab } from './InvestmentSettingsTab'
import { InvestmentTransactionsTab } from './InvestmentTransactionsTab'
import { INVESTMENT_TAB_COPY } from './model'
import type {
  AccountWithBalance,
  CreateAccountStatus,
  CreateTransactionStatus,
  CsvImportStatus,
  InvestmentTab,
  LoadStatus,
  TransactionDirectionFilter,
} from './types'
import { NestedTabButton, StatusCard } from './ui'

const ACCOUNT_TYPE_OPTIONS: AccountType[] = [
  'CASH',
  'DEPOSIT',
  'FUND',
  'IIS',
  'BROKERAGE',
]
const ACCOUNT_CURRENCY_OPTIONS: string[] = getAccountCurrencyOptions()
const DEFAULT_ACCOUNT_CURRENCY =
  ACCOUNT_CURRENCY_OPTIONS.includes('USD') &&
  ACCOUNT_CURRENCY_OPTIONS.length > 0
    ? 'USD'
    : (ACCOUNT_CURRENCY_OPTIONS[0] ?? 'USD')

interface InvestmentsViewProps {
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
  onUpdateAccount: (
    accountId: string,
    request: UpdateAccountRequest,
  ) => Promise<void>
  onDeleteAccount: (accountId: string) => Promise<void>
  createTransactionStatus: CreateTransactionStatus
  createTransactionErrorMessage: string | null
  onCreateTransaction: (
    accountId: string,
    request: CreateTransactionRequest,
  ) => Promise<boolean>
  onUpdateTransaction: (
    accountId: string,
    transactionId: string,
    request: UpdateTransactionRequest,
  ) => Promise<void>
  onDeleteTransaction: (
    accountId: string,
    transactionId: string,
  ) => Promise<void>
  csvImportStatus: CsvImportStatus
  csvImportErrorMessage: string | null
  csvImportResult: ImportTransactionsCsvResponse | null
  onImportTransactionsCsv: (accountId: string, file: File) => Promise<boolean>
  errorMessage: string | null
  onRetryAccounts: () => void
  onRetryTransactions: () => void
}

export function InvestmentsView({
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
  onUpdateAccount,
  onDeleteAccount,
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
}: InvestmentsViewProps) {
  const [activeInvestmentTab, setActiveInvestmentTab] = useState<InvestmentTab>(
    accounts.length === 0 ? 'settings' : 'overview',
  )
  const [newAccountName, setNewAccountName] = useState<string>('')
  const [newAccountCurrency, setNewAccountCurrency] = useState<string>(
    DEFAULT_ACCOUNT_CURRENCY,
  )
  const [newAccountType, setNewAccountType] = useState<AccountType>('CASH')
  const [editingAccountId, setEditingAccountId] = useState<string | null>(null)
  const [editingAccountName, setEditingAccountName] = useState<string>('')
  const [editingAccountType, setEditingAccountType] =
    useState<AccountType>('CASH')
  const [updateAccountStatus, setUpdateAccountStatus] =
    useState<CreateAccountStatus>('idle')
  const [updateAccountErrorMessage, setUpdateAccountErrorMessage] = useState<
    string | null
  >(null)
  const [deleteAccountStatus, setDeleteAccountStatus] =
    useState<CreateAccountStatus>('idle')
  const [deleteAccountErrorMessage, setDeleteAccountErrorMessage] = useState<
    string | null
  >(null)
  const [deleteAccountErrorAccountId, setDeleteAccountErrorAccountId] =
    useState<string | null>(null)
  const [newTransactionDate, setNewTransactionDate] =
    useState<string>(todayIsoDate())
  const [newTransactionDirection, setNewTransactionDirection] =
    useState<TransactionDirection>('OUTFLOW')
  const [newTransactionAmount, setNewTransactionAmount] = useState<string>('')
  const [newTransactionMemo, setNewTransactionMemo] = useState<string>('')
  const [editingTransactionAccountId, setEditingTransactionAccountId] =
    useState<string | null>(null)
  const [editingTransactionId, setEditingTransactionId] = useState<
    string | null
  >(null)
  const [editingTransactionDate, setEditingTransactionDate] =
    useState<string>(todayIsoDate())
  const [editingTransactionDirection, setEditingTransactionDirection] =
    useState<TransactionDirection>('OUTFLOW')
  const [editingTransactionAmount, setEditingTransactionAmount] =
    useState<string>('')
  const [editingTransactionMemo, setEditingTransactionMemo] =
    useState<string>('')
  const [updateTransactionStatus, setUpdateTransactionStatus] =
    useState<CreateTransactionStatus>('idle')
  const [updateTransactionErrorMessage, setUpdateTransactionErrorMessage] =
    useState<string | null>(null)
  const [deletingTransactionId, setDeletingTransactionId] = useState<
    string | null
  >(null)
  const [deleteTransactionErrorMessage, setDeleteTransactionErrorMessage] =
    useState<string | null>(null)
  const [csvFile, setCsvFile] = useState<File | null>(null)
  const csvFileInputRef = useRef<HTMLInputElement | null>(null)
  const activeInvestmentTabCopy = INVESTMENT_TAB_COPY[activeInvestmentTab]

  const resetEditingAccount = (): void => {
    setEditingAccountId(null)
    setEditingAccountName('')
    setEditingAccountType('CASH')
    setUpdateAccountStatus('idle')
    setUpdateAccountErrorMessage(null)
  }

  const resetDeleteAccountState = (): void => {
    setDeleteAccountStatus('idle')
    setDeleteAccountErrorMessage(null)
    setDeleteAccountErrorAccountId(null)
  }

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
    return (
      <StatusCard tone="neutral" message="Загружаем инвестиции и балансы..." />
    )
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
    newAccountName.trim().length > 0 &&
    isCurrencyValid &&
    createAccountStatus !== 'submitting'
  const isEditingSelectedAccount =
    selectedAccount !== null && editingAccountId === selectedAccount.id
  const canUpdateAccount =
    isEditingSelectedAccount &&
    editingAccountName.trim().length > 0 &&
    updateAccountStatus !== 'submitting'
  const isDeleteAvailabilityKnown = transactionsStatus === 'ready'
  const hasLoadedTransactions =
    isDeleteAvailabilityKnown && totalTransactionsCount > 0
  const canDeleteAccount =
    selectedAccount !== null &&
    !isEditingSelectedAccount &&
    isDeleteAvailabilityKnown &&
    totalTransactionsCount === 0 &&
    deleteAccountStatus !== 'submitting'

  const transactionDateCandidate = newTransactionDate.trim()
  const transactionAmountCandidate = normalizeMoneyInput(newTransactionAmount)
  const transactionMemoCandidate = newTransactionMemo.trim()
  const isTransactionDateValid = isValidIsoDateValue(transactionDateCandidate)
  const isTransactionAmountValid = isValidPositiveAmountValue(
    transactionAmountCandidate,
  )
  const canCreateTransaction =
    selectedAccount !== null &&
    isTransactionDateValid &&
    isTransactionAmountValid &&
    createTransactionStatus !== 'submitting'
  const editingTransactionDateCandidate = editingTransactionDate.trim()
  const editingTransactionAmountCandidate = normalizeMoneyInput(
    editingTransactionAmount,
  )
  const editingTransactionMemoCandidate = editingTransactionMemo.trim()
  const isEditingTransactionDateValid = isValidIsoDateValue(
    editingTransactionDateCandidate,
  )
  const isEditingTransactionAmountValid = isValidPositiveAmountValue(
    editingTransactionAmountCandidate,
  )
  const activeEditingTransactionId =
    editingTransactionAccountId === selectedAccountId
      ? editingTransactionId
      : null
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

  const handleCreateAccountSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
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

  const handleStartEditingAccount = (): void => {
    if (!selectedAccount) {
      return
    }

    resetDeleteAccountState()
    setEditingAccountId(selectedAccount.id)
    setEditingAccountName(selectedAccount.name)
    setEditingAccountType(selectedAccount.type)
    setUpdateAccountStatus('idle')
    setUpdateAccountErrorMessage(null)
  }

  const handleUpdateAccountSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !isEditingSelectedAccount || !canUpdateAccount) {
      return
    }

    resetDeleteAccountState()
    setUpdateAccountStatus('submitting')
    setUpdateAccountErrorMessage(null)

    try {
      await onUpdateAccount(selectedAccount.id, {
        name: editingAccountName.trim(),
        type: editingAccountType,
      })
      resetEditingAccount()
    } catch (error) {
      setUpdateAccountStatus('error')
      setUpdateAccountErrorMessage(toErrorMessage(error))
    }
  }

  const handleDeleteAccountClick = async (): Promise<void> => {
    if (!selectedAccount || !canDeleteAccount) {
      return
    }

    if (
      !window.confirm(
        `Удалить счет «${selectedAccount.name}»? Это действие необратимо.`,
      )
    ) {
      return
    }

    setDeleteAccountStatus('submitting')
    setDeleteAccountErrorMessage(null)

    try {
      await onDeleteAccount(selectedAccount.id)
      resetEditingAccount()
      resetEditingTransaction()
      resetDeleteTransactionState()
      resetDeleteAccountState()
    } catch (error) {
      setDeleteAccountStatus('error')
      setDeleteAccountErrorMessage(toErrorMessage(error))
      setDeleteAccountErrorAccountId(selectedAccount.id)
    }
  }

  const handleCreateTransactionSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !canCreateTransaction) {
      return
    }

    resetDeleteAccountState()
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

  const handleImportCsvSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    if (!selectedAccount || !csvFile || !canImportCsv) {
      return
    }

    resetDeleteAccountState()
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
    setEditingTransactionAmount(formatMoneyInput(transaction.amount))
    setEditingTransactionMemo(normalizeTransactionMemo(transaction.memo))
    setUpdateTransactionStatus('idle')
    setUpdateTransactionErrorMessage(null)
  }

  const handleUpdateTransactionSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    if (
      !selectedAccount ||
      !activeEditingTransactionId ||
      !canUpdateTransaction
    ) {
      return
    }

    resetDeleteAccountState()
    setUpdateTransactionStatus('submitting')
    setUpdateTransactionErrorMessage(null)

    try {
      await onUpdateTransaction(
        selectedAccount.id,
        activeEditingTransactionId,
        {
          occurredOn: editingTransactionDateCandidate,
          direction: editingTransactionDirection,
          amount: editingTransactionAmountCandidate,
          memo: editingTransactionMemoCandidate,
        },
      )
      resetEditingTransaction()
    } catch (error) {
      setUpdateTransactionStatus('error')
      setUpdateTransactionErrorMessage(toErrorMessage(error))
    }
  }

  const handleDeleteTransactionClick = async (
    transaction: TransactionDto,
  ): Promise<void> => {
    if (!selectedAccount || isDeleteTransactionSubmitting) {
      return
    }

    resetDeleteAccountState()
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
    resetEditingAccount()
    resetDeleteAccountState()
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
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-slate-50/70 p-4 lg:p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">
              Инвестиции
            </h2>
            <p className="mt-1 max-w-3xl text-sm text-slate-600">
              Раздел показывает truthful cash view по инвестиционным счетам:
              обзор структуры портфеля, ручной ввод покупок и продаж, а также
              настройки самих счетов.
            </p>
          </div>
          <p className="text-xs uppercase tracking-wide text-slate-500">
            {accounts.length === 0
              ? 'Нет счетов'
              : `${accounts.length} ${toInvestmentAccountCountLabel(accounts.length)}`}
          </p>
        </div>
      </section>

      <section className="rounded-3xl border border-slate-200 bg-white p-4 lg:p-5">
        <nav className="flex flex-wrap rounded-2xl border border-slate-200 bg-slate-50 p-1">
          <NestedTabButton
            label="Обзор"
            isActive={activeInvestmentTab === 'overview'}
            onClick={() => setActiveInvestmentTab('overview')}
          />
          <NestedTabButton
            label="Транзакции"
            isActive={activeInvestmentTab === 'transactions'}
            onClick={() => setActiveInvestmentTab('transactions')}
          />
          <NestedTabButton
            label="Настройки"
            isActive={activeInvestmentTab === 'settings'}
            onClick={() => setActiveInvestmentTab('settings')}
          />
        </nav>

        <div className="mt-4 border-t border-slate-200 pt-4">
          <h3 className="text-base font-semibold text-slate-900">
            {activeInvestmentTabCopy.title}
          </h3>
          <p className="mt-1 text-sm text-slate-600">
            {activeInvestmentTabCopy.description}
          </p>
        </div>
      </section>

      {activeInvestmentTab === 'overview' ? (
        <InvestmentOverviewTab
          accounts={accounts}
          selectedAccountId={selectedAccountId}
          onOpenTransactions={(accountId) => {
            handleSelectAccountClick(accountId)
            setActiveInvestmentTab('transactions')
          }}
          onOpenSettings={(accountId) => {
            handleSelectAccountClick(accountId)
            setActiveInvestmentTab('settings')
          }}
          onOpenSettingsWithoutSelection={() => setActiveInvestmentTab('settings')}
        />
      ) : null}

      {activeInvestmentTab === 'transactions' ? (
        <InvestmentTransactionsTab
          accounts={accounts}
          selectedAccountId={selectedAccountId}
          selectedAccount={selectedAccount}
          transactionsStatus={transactionsStatus}
          transactionsErrorMessage={transactionsErrorMessage}
          filteredTransactions={filteredTransactions}
          totalTransactionsCount={totalTransactionsCount}
          directionFilter={directionFilter}
          memoFilter={memoFilter}
          onDirectionFilterChange={onDirectionFilterChange}
          onMemoFilterChange={onMemoFilterChange}
          onSelectAccount={handleSelectAccountClick}
          createTransactionStatus={createTransactionStatus}
          createTransactionErrorMessage={createTransactionErrorMessage}
          newTransactionDate={newTransactionDate}
          newTransactionDirection={newTransactionDirection}
          newTransactionAmount={newTransactionAmount}
          newTransactionMemo={newTransactionMemo}
          onNewTransactionDateChange={setNewTransactionDate}
          onNewTransactionDirectionChange={setNewTransactionDirection}
          onNewTransactionAmountChange={setNewTransactionAmount}
          onNewTransactionMemoChange={setNewTransactionMemo}
          isTransactionAmountValid={isTransactionAmountValid}
          canCreateTransaction={canCreateTransaction}
          onCreateTransactionSubmit={handleCreateTransactionSubmit}
          activeEditingTransactionId={activeEditingTransactionId}
          editingTransactionDate={editingTransactionDate}
          editingTransactionDirection={editingTransactionDirection}
          editingTransactionAmount={editingTransactionAmount}
          editingTransactionMemo={editingTransactionMemo}
          onEditingTransactionDateChange={setEditingTransactionDate}
          onEditingTransactionDirectionChange={setEditingTransactionDirection}
          onEditingTransactionAmountChange={setEditingTransactionAmount}
          onEditingTransactionMemoChange={setEditingTransactionMemo}
          isEditingTransactionAmountValid={isEditingTransactionAmountValid}
          canUpdateTransaction={canUpdateTransaction}
          updateTransactionStatus={updateTransactionStatus}
          updateTransactionErrorMessage={updateTransactionErrorMessage}
          deletingTransactionId={deletingTransactionId}
          deleteTransactionErrorMessage={deleteTransactionErrorMessage}
          isDeleteTransactionSubmitting={isDeleteTransactionSubmitting}
          onStartEditingTransaction={handleStartEditingTransaction}
          onUpdateTransactionSubmit={handleUpdateTransactionSubmit}
          onCancelEditingTransaction={resetEditingTransaction}
          onDeleteTransaction={handleDeleteTransactionClick}
          onRetryTransactions={onRetryTransactions}
          onOpenSettings={() => setActiveInvestmentTab('settings')}
        />
      ) : null}

      {activeInvestmentTab === 'settings' ? (
        <InvestmentSettingsTab
          accounts={accounts}
          accountTypeOptions={ACCOUNT_TYPE_OPTIONS}
          accountCurrencyOptions={ACCOUNT_CURRENCY_OPTIONS}
          selectedAccountId={selectedAccountId}
          selectedAccount={selectedAccount}
          isCurrencyValid={isCurrencyValid}
          createAccountStatus={createAccountStatus}
          createAccountErrorMessage={createAccountErrorMessage}
          newAccountName={newAccountName}
          newAccountCurrency={newAccountCurrency}
          newAccountType={newAccountType}
          onNewAccountNameChange={setNewAccountName}
          onNewAccountCurrencyChange={setNewAccountCurrency}
          onNewAccountTypeChange={setNewAccountType}
          canCreate={canCreate}
          onCreateAccountSubmit={handleCreateAccountSubmit}
          onSelectAccount={handleSelectAccountClick}
          isEditingSelectedAccount={isEditingSelectedAccount}
          editingAccountName={editingAccountName}
          editingAccountType={editingAccountType}
          onEditingAccountNameChange={setEditingAccountName}
          onEditingAccountTypeChange={setEditingAccountType}
          canUpdateAccount={canUpdateAccount}
          updateAccountStatus={updateAccountStatus}
          updateAccountErrorMessage={updateAccountErrorMessage}
          onStartEditingAccount={handleStartEditingAccount}
          onUpdateAccountSubmit={handleUpdateAccountSubmit}
          onCancelEditingAccount={resetEditingAccount}
          canDeleteAccount={canDeleteAccount}
          deleteAccountStatus={deleteAccountStatus}
          deleteAccountErrorMessage={deleteAccountErrorMessage}
          deleteAccountErrorAccountId={deleteAccountErrorAccountId}
          isDeleteAvailabilityKnown={isDeleteAvailabilityKnown}
          hasLoadedTransactions={hasLoadedTransactions}
          onDeleteAccount={handleDeleteAccountClick}
          onOpenTransactions={() => setActiveInvestmentTab('transactions')}
          csvImportStatus={csvImportStatus}
          csvImportErrorMessage={csvImportErrorMessage}
          csvImportResult={csvImportResult}
          csvFile={csvFile}
          csvFileInputRef={csvFileInputRef}
          onCsvFileChange={setCsvFile}
          canImportCsv={canImportCsv}
          onImportCsvSubmit={handleImportCsvSubmit}
        />
      ) : null}
    </div>
  )
}

function toErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Произошла ошибка. Попробуйте еще раз.'
}

function getAccountCurrencyOptions(): string[] {
  const fallbackOptions = ['USD', 'EUR', 'RUB']

  const intlWithSupportedValues = Intl as typeof Intl & {
    supportedValuesOf?: (key: 'currency') => string[]
  }
  const dynamicOptions = intlWithSupportedValues.supportedValuesOf?.('currency')
  if (!dynamicOptions || dynamicOptions.length === 0) {
    return fallbackOptions
  }

  return [
    ...new Set(
      dynamicOptions.map((currencyCode) => currencyCode.toUpperCase()),
    ),
  ].sort((left, right) => left.localeCompare(right))
}
