import type { TransactionDto } from '../../api'
import { formatMoneyInput } from '../../money-input'
import { InstrumentSearchField } from './InstrumentSearchField'
import {
  formatAmount,
  formatIsoDateRu,
  formatSignedAmount,
  isMoexTradeAccountType,
  toDirectionLabel,
  toTransactionMemoDisplay,
} from './formatting'
import {
  formatDecimalInput,
  type TransactionTradeDraft,
  type TransactionTradeEvaluation,
} from './transactionTrade'
import type {
  AccountWithBalance,
  CreateTransactionStatus,
  LoadStatus,
  TransactionDirectionFilter,
} from './types'
import { StatusCard } from './ui'

interface InvestmentTransactionsTabProps {
  accounts: AccountWithBalance[]
  transactionsStatus: LoadStatus
  transactionsErrorMessage: string | null
  filteredTransactions: TransactionDto[]
  totalTransactionsCount: number
  directionFilter: TransactionDirectionFilter
  memoFilter: string
  onDirectionFilterChange: (value: TransactionDirectionFilter) => void
  onMemoFilterChange: (value: string) => void
  createTransactionStatus: CreateTransactionStatus
  createTransactionErrorMessage: string | null
  newTransactionDraft: TransactionTradeDraft
  newTransactionEvaluation: TransactionTradeEvaluation
  newTransactionAccount: AccountWithBalance | null
  onNewTransactionDraftChange: (draft: TransactionTradeDraft) => void
  onNewTransactionAccountChange: (accountId: string) => void
  canCreateTransaction: boolean
  onCreateTransactionSubmit: (
    options?: { resetAfterSave: boolean },
  ) => Promise<void>
  onResetNewTransaction: () => void
  activeEditingTransactionId: string | null
  editingTransactionDraft: TransactionTradeDraft
  editingTransactionEvaluation: TransactionTradeEvaluation
  editingTransactionAccount: AccountWithBalance | null
  onEditingTransactionDraftChange: (draft: TransactionTradeDraft) => void
  canUpdateTransaction: boolean
  updateTransactionStatus: CreateTransactionStatus
  updateTransactionErrorMessage: string | null
  deletingTransactionId: string | null
  deleteTransactionErrorMessage: string | null
  isDeleteTransactionSubmitting: boolean
  onStartEditingTransaction: (transaction: TransactionDto) => void
  onUpdateTransactionSubmit: () => Promise<void>
  onCancelEditingTransaction: () => void
  onDeleteTransaction: (transaction: TransactionDto) => Promise<void>
  onRetryTransactions: () => void
  onOpenSettings: () => void
}

export function InvestmentTransactionsTab({
  accounts,
  transactionsStatus,
  transactionsErrorMessage,
  filteredTransactions,
  totalTransactionsCount,
  directionFilter,
  memoFilter,
  onDirectionFilterChange,
  onMemoFilterChange,
  createTransactionStatus,
  createTransactionErrorMessage,
  newTransactionDraft,
  newTransactionEvaluation,
  newTransactionAccount,
  onNewTransactionDraftChange,
  onNewTransactionAccountChange,
  canCreateTransaction,
  onCreateTransactionSubmit,
  onResetNewTransaction,
  activeEditingTransactionId,
  editingTransactionDraft,
  editingTransactionEvaluation,
  editingTransactionAccount,
  onEditingTransactionDraftChange,
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

  const supportedTransactionAccounts = accounts.filter((account) =>
    isMoexTradeAccountType(account.type),
  )

  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-sm font-semibold text-slate-900">
            Все инвестиционные транзакции
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            Глобальный список покупок и продаж по всем инвестиционным счетам.
          </p>
        </div>

        <button
          type="button"
          onClick={onOpenSettings}
          className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800"
        >
          Настроить счета
        </button>
      </div>

      <TransactionTradeForm
        title="Добавить транзакцию"
        accounts={supportedTransactionAccounts}
        selectedAccountId={newTransactionDraft.accountId}
        onSelectedAccountIdChange={onNewTransactionAccountChange}
        currency={newTransactionAccount?.balance.currency ?? '—'}
        draft={newTransactionDraft}
        evaluation={newTransactionEvaluation}
        onDraftChange={onNewTransactionDraftChange}
        canSubmit={canCreateTransaction}
        submitLabel={
          createTransactionStatus === 'submitting' ? 'Сохраняем...' : 'Сохранить'
        }
        secondarySubmitLabel="Сохранить и добавить еще"
        onSubmit={() => {
          void onCreateTransactionSubmit()
        }}
        onSecondarySubmit={() => {
          void onCreateTransactionSubmit({ resetAfterSave: true })
        }}
        onCancel={onResetNewTransaction}
        errorMessage={
          createTransactionStatus === 'error'
            ? createTransactionErrorMessage
            : null
        }
        className="mt-4"
      />

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
            <option value="INFLOW">Продажи</option>
            <option value="OUTFLOW">Покупки</option>
          </select>
        </label>

        <label className="text-xs text-slate-600">
          Фильтр по заметкам
          <input
            type="text"
            value={memoFilter}
            onChange={(event) => onMemoFilterChange(event.target.value)}
            placeholder="Поиск по заметкам"
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
            message={
              transactionsErrorMessage ?? 'Не удалось загрузить транзакции.'
            }
            actionLabel="Повторить"
            onAction={onRetryTransactions}
          />
        ) : null}

        {transactionsStatus === 'ready' && totalTransactionsCount === 0 ? (
          <StatusCard
            tone="neutral"
            message="Пока нет инвестиционных транзакций."
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

        {transactionsStatus === 'ready' && filteredTransactions.length > 0 ? (
          <ul className="space-y-2">
            {filteredTransactions.map((transaction) => {
              const isEditing = activeEditingTransactionId === transaction.id
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
                    <TransactionTradeForm
                      title="Редактировать транзакцию"
                      accounts={accounts}
                      selectedAccountId={editingTransactionAccount?.id ?? null}
                      currency={
                        editingTransactionAccount?.balance.currency ??
                        transaction.currency
                      }
                      draft={editingTransactionDraft}
                      evaluation={editingTransactionEvaluation}
                      onDraftChange={onEditingTransactionDraftChange}
                      canSubmit={canUpdateTransaction}
                      submitLabel={
                        updateTransactionStatus === 'submitting'
                          ? 'Сохраняем...'
                          : 'Сохранить'
                      }
                      onSubmit={() => {
                        void onUpdateTransactionSubmit()
                      }}
                      onCancel={onCancelEditingTransaction}
                      errorMessage={
                        updateTransactionStatus === 'error'
                          ? updateTransactionErrorMessage
                          : null
                      }
                    />
                  ) : (
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-medium text-slate-900">
                          {toTransactionTitle(transaction)}
                        </p>
                        <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                          {transaction.accountName}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          {formatIsoDateRu(transaction.occurredOn)} ·{' '}
                          {toDirectionLabel(transaction.direction)}
                          {transaction.quantity && transaction.unitPrice ? (
                            <>
                              {' '}
                              · {formatDecimalDisplay(transaction.quantity)} ×{' '}
                              {formatAmount(transaction.unitPrice)}
                            </>
                          ) : null}
                          {transaction.feeAmount ? (
                            <> · Комиссия {formatAmount(transaction.feeAmount)}</>
                          ) : null}
                        </p>
                        {transaction.memo ? (
                          <p className="mt-2 text-xs text-slate-500">
                            {toTransactionMemoDisplay(transaction.memo)}
                          </p>
                        ) : null}
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
                            onClick={() => onStartEditingTransaction(transaction)}
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
                            {isDeletingThisTransaction ? 'Удаляем...' : 'Удалить'}
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
    </article>
  )
}

interface TransactionTradeFormProps {
  title: string
  accounts: AccountWithBalance[]
  selectedAccountId: string | null
  onSelectedAccountIdChange?: (accountId: string) => void
  currency: string
  draft: TransactionTradeDraft
  evaluation: TransactionTradeEvaluation
  onDraftChange: (draft: TransactionTradeDraft) => void
  canSubmit: boolean
  submitLabel: string
  onSubmit: () => void
  onCancel: () => void
  onSecondarySubmit?: () => void
  secondarySubmitLabel?: string
  errorMessage: string | null
  className?: string
}

function TransactionTradeForm({
  title,
  accounts,
  selectedAccountId,
  onSelectedAccountIdChange,
  currency,
  draft,
  evaluation,
  onDraftChange,
  canSubmit,
  submitLabel,
  onSubmit,
  onCancel,
  onSecondarySubmit,
  secondarySubmitLabel,
  errorMessage,
  className,
}: TransactionTradeFormProps) {
  const isCreateForm = onSelectedAccountIdChange !== undefined
  const selectedAccount =
    accounts.find((account) => account.id === selectedAccountId) ?? null
  const patchDraft = (patch: Partial<TransactionTradeDraft>): void => {
    onDraftChange({
      ...draft,
      ...patch,
    })
  }

  const validationMessages = [
    !evaluation.isInstrumentSymbolValid && draft.instrumentSymbol.trim().length > 0
      ? 'Укажите непустой символ инструмента.'
      : null,
    !evaluation.isUnitPriceValid && draft.unitPrice.trim().length > 0
      ? 'Цена должна быть положительной и содержать не более 2 знаков после точки.'
      : null,
    !evaluation.isQuantityValid && draft.quantity.trim().length > 0
      ? 'Количество должно быть положительным и содержать не более 6 знаков после точки.'
      : null,
    !evaluation.isFeeAmountValid && draft.feeAmount.trim().length > 0
      ? 'Комиссия не может быть отрицательной и должна содержать не более 2 знаков после точки.'
      : null,
    !evaluation.isTotalAmountValid &&
    evaluation.isUnitPriceValid &&
    evaluation.isQuantityValid &&
    evaluation.isFeeAmountValid
      ? 'Итог должен точно укладываться в валютную сумму с 2 знаками после точки.'
      : null,
  ].filter((message): message is string => message !== null)

  if (isCreateForm && accounts.length === 0) {
    return (
      <section
        className={`rounded-lg border border-slate-200 bg-white p-4 ${className ?? ''}`}
      >
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
          {title}
        </p>
        <StatusCard
          tone="neutral"
          message="Для выбора инструмента с Московской биржи нужен брокерский счет или ИИС."
        />
      </section>
    )
  }

  return (
    <section
      className={`rounded-lg border border-slate-200 bg-white p-4 ${className ?? ''}`}
    >
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        {title}
      </p>

      <label className="mt-4 block text-xs text-slate-600">
        Счет
        {onSelectedAccountIdChange ? (
          <select
            value={selectedAccountId ?? ''}
            onChange={(event) => onSelectedAccountIdChange(event.target.value)}
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
          >
            <option value="" disabled>
              Выберите счет
            </option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name} · {account.balance.currency}
              </option>
            ))}
          </select>
        ) : (
          <div className="mt-1 rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
            {selectedAccount
              ? `${selectedAccount.name} · ${selectedAccount.balance.currency}`
              : 'Счет не найден'}
          </div>
        )}
      </label>

      <div className="mt-3">
        <p className="text-xs text-slate-600">Направление</p>
        <div className="mt-1 grid grid-cols-2 gap-2">
          <DirectionButton
            label="Продать"
            isActive={draft.direction === 'INFLOW'}
            tone="sell"
            onClick={() => patchDraft({ direction: 'INFLOW' })}
          />
          <DirectionButton
            label="Купить"
            isActive={draft.direction === 'OUTFLOW'}
            tone="buy"
            onClick={() => patchDraft({ direction: 'OUTFLOW' })}
          />
        </div>
      </div>

      <label className="mt-4 block text-xs text-slate-600">
        Инструмент
        <InstrumentSearchField
          key={selectedAccountId ?? 'unselected-account'}
          account={selectedAccount}
          value={draft.instrumentSymbol}
          onChange={(instrumentSymbol) =>
            patchDraft({
              instrumentSymbol,
            })
          }
        />
      </label>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <label className="text-xs text-slate-600">
          Дата
          <input
            type="date"
            value={draft.occurredOn}
            onChange={(event) =>
              patchDraft({
                occurredOn: event.target.value,
              })
            }
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
          />
        </label>

        <label className="text-xs text-slate-600">
          Цена
          <input
            type="text"
            inputMode="decimal"
            value={draft.unitPrice}
            onChange={(event) =>
              patchDraft({
                unitPrice: formatMoneyInput(event.target.value),
              })
            }
            placeholder="0.00"
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
          />
        </label>
      </div>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <label className="text-xs text-slate-600">
          Количество
          <input
            type="text"
            inputMode="decimal"
            value={draft.quantity}
            onChange={(event) =>
              patchDraft({
                quantity: formatDecimalInput(event.target.value, 6),
              })
            }
            placeholder="1"
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
          />
        </label>

        <label className="text-xs text-slate-600">
          Комиссия
          <input
            type="text"
            inputMode="decimal"
            value={draft.feeAmount}
            onChange={(event) =>
              patchDraft({
                feeAmount: formatMoneyInput(event.target.value),
              })
            }
            placeholder="0.00"
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
          />
        </label>
      </div>

      <label className="mt-3 block text-xs text-slate-600">
        Заметки
        <div className="mt-1 flex items-center justify-between text-[11px] text-slate-400">
          <span>Опционально</span>
          <span>{draft.memo.length}/128</span>
        </div>
        <textarea
          value={draft.memo}
          onChange={(event) =>
            patchDraft({
              memo: event.target.value,
            })
          }
          maxLength={128}
          rows={4}
          placeholder="Комментарии"
          className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
        />
      </label>

      <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
        <p className="text-xs uppercase tracking-wide text-slate-500">Итого</p>
        <div className="mt-1 flex items-baseline justify-between gap-3">
          <p
            className={`text-lg font-semibold ${
              draft.direction === 'OUTFLOW' ? 'text-rose-700' : 'text-emerald-700'
            }`}
          >
            {evaluation.totalAmount
              ? formatSignedAmount(evaluation.totalAmount, draft.direction)
              : '0,00'}
          </p>
          <p className="text-xs uppercase tracking-wide text-slate-500">
            {currency}
          </p>
        </div>
      </div>

      {validationMessages.length > 0 ? (
        <div className="mt-3 space-y-1">
          {validationMessages.map((message) => (
            <p key={message} className="text-xs text-amber-700">
              {message}
            </p>
          ))}
        </div>
      ) : null}

      {errorMessage ? (
        <p className="mt-3 text-xs text-amber-700">{errorMessage}</p>
      ) : null}

      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600"
        >
          Отмена
        </button>
        <button
          type="button"
          disabled={!canSubmit}
          onClick={onSubmit}
          className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitLabel}
        </button>
        {onSecondarySubmit && secondarySubmitLabel ? (
          <button
            type="button"
            disabled={!canSubmit}
            onClick={onSecondarySubmit}
            className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {secondarySubmitLabel}
          </button>
        ) : null}
      </div>
    </section>
  )
}

interface DirectionButtonProps {
  label: string
  isActive: boolean
  tone: 'buy' | 'sell'
  onClick: () => void
}

function DirectionButton({
  label,
  isActive,
  tone,
  onClick,
}: DirectionButtonProps) {
  const activeClassName =
    tone === 'buy'
      ? 'border-blue-500 bg-blue-50 text-blue-700'
      : 'border-rose-500 bg-rose-50 text-rose-700'

  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-md border px-3 py-2 text-sm font-medium ${
        isActive
          ? activeClassName
          : 'border-slate-300 bg-white text-slate-600'
      }`}
    >
      {label}
    </button>
  )
}

function toTransactionTitle(transaction: TransactionDto): string {
  if (transaction.instrumentSymbol) {
    return transaction.instrumentSymbol
  }

  return toTransactionMemoDisplay(transaction.memo)
}

function formatDecimalDisplay(value: string): string {
  const [integerPart, fractionPart = ''] = value.split('.')
  const trimmedFraction = fractionPart.replace(/0+$/, '')

  return trimmedFraction.length > 0
    ? `${integerPart},${trimmedFraction}`
    : integerPart
}
