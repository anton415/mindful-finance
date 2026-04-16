import { type FormEvent } from 'react'
import type {
  TransactionDirection,
  TransactionDto,
} from '../../api'
import { formatMoneyInput } from '../../money-input'
import {
  formatIsoDateRu,
  formatSignedAmount,
  toDirectionLabel,
  toDirectionSelectLabel,
  toTransactionMemoDisplay,
} from './formatting'
import type {
  AccountWithBalance,
  CreateTransactionStatus,
  LoadStatus,
  TransactionDirectionFilter,
} from './types'
import { InvestmentAccountSelectorList, StatusCard } from './ui'

interface InvestmentTransactionsTabProps {
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
  createTransactionStatus: CreateTransactionStatus
  createTransactionErrorMessage: string | null
  newTransactionDate: string
  newTransactionDirection: TransactionDirection
  newTransactionAmount: string
  newTransactionMemo: string
  onNewTransactionDateChange: (value: string) => void
  onNewTransactionDirectionChange: (value: TransactionDirection) => void
  onNewTransactionAmountChange: (value: string) => void
  onNewTransactionMemoChange: (value: string) => void
  isTransactionAmountValid: boolean
  canCreateTransaction: boolean
  onCreateTransactionSubmit: (
    event: FormEvent<HTMLFormElement>,
  ) => Promise<void>
  activeEditingTransactionId: string | null
  editingTransactionDate: string
  editingTransactionDirection: TransactionDirection
  editingTransactionAmount: string
  editingTransactionMemo: string
  onEditingTransactionDateChange: (value: string) => void
  onEditingTransactionDirectionChange: (value: TransactionDirection) => void
  onEditingTransactionAmountChange: (value: string) => void
  onEditingTransactionMemoChange: (value: string) => void
  isEditingTransactionAmountValid: boolean
  canUpdateTransaction: boolean
  updateTransactionStatus: CreateTransactionStatus
  updateTransactionErrorMessage: string | null
  deletingTransactionId: string | null
  deleteTransactionErrorMessage: string | null
  isDeleteTransactionSubmitting: boolean
  onStartEditingTransaction: (transaction: TransactionDto) => void
  onUpdateTransactionSubmit: (
    event: FormEvent<HTMLFormElement>,
  ) => Promise<void>
  onCancelEditingTransaction: () => void
  onDeleteTransaction: (transaction: TransactionDto) => Promise<void>
  onRetryTransactions: () => void
  onOpenSettings: () => void
}

export function InvestmentTransactionsTab({
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
  createTransactionStatus,
  createTransactionErrorMessage,
  newTransactionDate,
  newTransactionDirection,
  newTransactionAmount,
  newTransactionMemo,
  onNewTransactionDateChange,
  onNewTransactionDirectionChange,
  onNewTransactionAmountChange,
  onNewTransactionMemoChange,
  isTransactionAmountValid,
  canCreateTransaction,
  onCreateTransactionSubmit,
  activeEditingTransactionId,
  editingTransactionDate,
  editingTransactionDirection,
  editingTransactionAmount,
  editingTransactionMemo,
  onEditingTransactionDateChange,
  onEditingTransactionDirectionChange,
  onEditingTransactionAmountChange,
  onEditingTransactionMemoChange,
  isEditingTransactionAmountValid,
  canUpdateTransaction,
  updateTransactionStatus,
  updateTransactionErrorMessage,
  deletingTransactionId,
  deleteTransactionErrorMessage,
  isDeleteTransactionSubmitting,
  onStartEditingTransaction,
  onUpdateTransactionSubmit,
  onCancelEditingTransaction,
  onDeleteTransaction,
  onRetryTransactions,
  onOpenSettings,
}: InvestmentTransactionsTabProps) {
  if (accounts.length === 0) {
    return (
      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <StatusCard
          tone="neutral"
          message="Пока нет счетов для внесения покупок и продаж. Сначала добавьте счет в настройках."
        />
        <button
          type="button"
          onClick={onOpenSettings}
          className="mt-4 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800"
        >
          Открыть настройки
        </button>
      </article>
    )
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.3fr)]">
      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <h2 className="text-sm font-semibold text-slate-900">
          Инвестиционные счета
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Выберите счет, чтобы посмотреть и изменить его транзакции.
        </p>

        <InvestmentAccountSelectorList
          accounts={accounts}
          selectedAccountId={selectedAccountId}
          onSelectAccount={onSelectAccount}
          emptyMessage="Пока нет счетов."
        />
      </article>

      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        {!selectedAccount ? (
          <StatusCard
            tone="neutral"
            message="Выберите счет, чтобы посмотреть транзакции."
          />
        ) : (
          <>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">
                  {selectedAccount.name}
                </h2>
                <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                  Транзакции · {selectedAccount.balance.currency}
                </p>
              </div>

              <button
                type="button"
                onClick={onOpenSettings}
                className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800"
              >
                Настроить счет
              </button>
            </div>

            <form
              className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
              onSubmit={(event) => {
                void onCreateTransactionSubmit(event)
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
                    onChange={(event) =>
                      onNewTransactionDateChange(event.target.value)
                    }
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>

                <label className="text-xs text-slate-600">
                  Направление
                  <select
                    value={newTransactionDirection}
                    onChange={(event) =>
                      onNewTransactionDirectionChange(
                        event.target.value as TransactionDirection,
                      )
                    }
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  >
                    <option value="OUTFLOW">
                      {toDirectionSelectLabel('OUTFLOW')}
                    </option>
                    <option value="INFLOW">
                      {toDirectionSelectLabel('INFLOW')}
                    </option>
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
                    onChange={(event) =>
                      onNewTransactionAmountChange(
                        formatMoneyInput(event.target.value),
                      )
                    }
                    placeholder="0,00"
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>

                <label className="text-xs text-slate-600">
                  Описание
                  <input
                    type="text"
                    value={newTransactionMemo}
                    onChange={(event) =>
                      onNewTransactionMemoChange(event.target.value)
                    }
                    placeholder="Покупка фонда"
                    className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                  />
                </label>
              </div>

              {!isTransactionAmountValid &&
              newTransactionAmount.trim().length > 0 ? (
                <p className="mt-2 text-xs text-amber-700">
                  Сумма должна быть положительной и содержать не более 2 знаков
                  после запятой.
                </p>
              ) : null}

              {createTransactionStatus === 'error' &&
              createTransactionErrorMessage ? (
                <p className="mt-2 text-xs text-amber-700">
                  {createTransactionErrorMessage}
                </p>
              ) : null}

              <button
                type="submit"
                disabled={!canCreateTransaction}
                className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {createTransactionStatus === 'submitting'
                  ? 'Добавляем...'
                  : 'Добавить транзакцию'}
              </button>
            </form>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <label className="text-xs text-slate-600">
                Направление
                <select
                  value={directionFilter}
                  onChange={(event) =>
                    onDirectionFilterChange(
                      event.target.value as TransactionDirectionFilter,
                    )
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
              {transactionsStatus === 'loading' ||
              transactionsStatus === 'idle' ? (
                <StatusCard tone="neutral" message="Загружаем транзакции..." />
              ) : null}

              {transactionsStatus === 'error' ? (
                <StatusCard
                  tone="warning"
                  message={
                    transactionsErrorMessage ??
                    'Не удалось загрузить транзакции.'
                  }
                  actionLabel="Повторить"
                  onAction={onRetryTransactions}
                />
              ) : null}

              {transactionsStatus === 'ready' &&
              totalTransactionsCount === 0 ? (
                <StatusCard
                  tone="neutral"
                  message="Для этого счета пока нет транзакций."
                />
              ) : null}

              {transactionsStatus === 'ready' &&
              totalTransactionsCount > 0 &&
              filteredTransactions.length === 0 ? (
                <StatusCard
                  tone="neutral"
                  message="Нет транзакций, подходящих под фильтры."
                />
              ) : null}

              {transactionsStatus === 'ready' &&
              filteredTransactions.length > 0 ? (
                <ul className="space-y-2">
                  {filteredTransactions.map((transaction) => {
                    const isEditing =
                      activeEditingTransactionId === transaction.id
                    const hasDeleteError =
                      deletingTransactionId === transaction.id &&
                      deleteTransactionErrorMessage !== null
                    const isDeletingThisTransaction =
                      deletingTransactionId === transaction.id &&
                      deleteTransactionErrorMessage === null

                    return (
                      <li
                        key={transaction.id}
                        className="rounded-lg border border-slate-200 bg-white px-3 py-3"
                      >
                        {isEditing ? (
                          <form
                            onSubmit={(event) => {
                              void onUpdateTransactionSubmit(event)
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
                                  onChange={(event) =>
                                    onEditingTransactionDateChange(
                                      event.target.value,
                                    )
                                  }
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>

                              <label className="text-xs text-slate-600">
                                Направление
                                <select
                                  value={editingTransactionDirection}
                                  onChange={(event) =>
                                    onEditingTransactionDirectionChange(
                                      event.target
                                        .value as TransactionDirection,
                                    )
                                  }
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                >
                                  <option value="OUTFLOW">
                                    {toDirectionSelectLabel('OUTFLOW')}
                                  </option>
                                  <option value="INFLOW">
                                    {toDirectionSelectLabel('INFLOW')}
                                  </option>
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
                                  onChange={(event) =>
                                    onEditingTransactionAmountChange(
                                      formatMoneyInput(event.target.value),
                                    )
                                  }
                                  placeholder="0,00"
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>

                              <label className="text-xs text-slate-600">
                                Описание
                                <input
                                  type="text"
                                  value={editingTransactionMemo}
                                  onChange={(event) =>
                                    onEditingTransactionMemoChange(
                                      event.target.value,
                                    )
                                  }
                                  placeholder="Без описания"
                                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                                />
                              </label>
                            </div>

                            {!isEditingTransactionAmountValid &&
                            editingTransactionAmount.trim().length > 0 ? (
                              <p className="mt-2 text-xs text-amber-700">
                                Сумма должна быть положительной и содержать не
                                более 2 знаков после запятой.
                              </p>
                            ) : null}

                            {updateTransactionStatus === 'error' &&
                            updateTransactionErrorMessage ? (
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
                                onClick={onCancelEditingTransaction}
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
                                {formatSignedAmount(
                                  transaction.amount,
                                  transaction.direction,
                                )}
                              </p>
                              <div className="mt-2 flex flex-wrap justify-end gap-2">
                                <button
                                  type="button"
                                  onClick={() =>
                                    onStartEditingTransaction(transaction)
                                  }
                                  disabled={isDeleteTransactionSubmitting}
                                  className="rounded-md border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  Редактировать
                                </button>
                                <button
                                  type="button"
                                  onClick={() => {
                                    void onDeleteTransaction(transaction)
                                  }}
                                  disabled={isDeleteTransactionSubmitting}
                                  className="rounded-md border border-rose-200 px-2.5 py-1 text-xs font-medium text-rose-700 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  {isDeletingThisTransaction
                                    ? 'Удаляем...'
                                    : 'Удалить'}
                                </button>
                              </div>
                              {hasDeleteError ? (
                                <p className="mt-2 text-xs text-amber-700">
                                  {deleteTransactionErrorMessage}
                                </p>
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
