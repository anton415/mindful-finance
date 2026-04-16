import { type FormEvent } from 'react'
import type {
  AccountType,
  ImportTransactionsCsvResponse,
} from '../../api'
import { toAccountTypeLabel } from './formatting'
import type {
  AccountWithBalance,
  CreateAccountStatus,
  CsvImportStatus,
} from './types'
import { InvestmentAccountSelectorList, StatusCard } from './ui'

interface InvestmentSettingsTabProps {
  accounts: AccountWithBalance[]
  accountTypeOptions: AccountType[]
  accountCurrencyOptions: string[]
  selectedAccountId: string | null
  selectedAccount: AccountWithBalance | null
  isCurrencyValid: boolean
  createAccountStatus: CreateAccountStatus
  createAccountErrorMessage: string | null
  newAccountName: string
  newAccountCurrency: string
  newAccountType: AccountType
  onNewAccountNameChange: (value: string) => void
  onNewAccountCurrencyChange: (value: string) => void
  onNewAccountTypeChange: (value: AccountType) => void
  canCreate: boolean
  onCreateAccountSubmit: (
    event: FormEvent<HTMLFormElement>,
  ) => Promise<void>
  onSelectAccount: (accountId: string) => void
  isEditingSelectedAccount: boolean
  editingAccountName: string
  editingAccountType: AccountType
  onEditingAccountNameChange: (value: string) => void
  onEditingAccountTypeChange: (value: AccountType) => void
  canUpdateAccount: boolean
  updateAccountStatus: CreateAccountStatus
  updateAccountErrorMessage: string | null
  onStartEditingAccount: () => void
  onUpdateAccountSubmit: (
    event: FormEvent<HTMLFormElement>,
  ) => Promise<void>
  onCancelEditingAccount: () => void
  canDeleteAccount: boolean
  deleteAccountStatus: CreateAccountStatus
  deleteAccountErrorMessage: string | null
  deleteAccountErrorAccountId: string | null
  isDeleteAvailabilityKnown: boolean
  hasLoadedTransactions: boolean
  onDeleteAccount: () => Promise<void>
  onOpenTransactions: () => void
  csvImportStatus: CsvImportStatus
  csvImportErrorMessage: string | null
  csvImportResult: ImportTransactionsCsvResponse | null
  csvFile: File | null
  csvFileInputRef: { current: HTMLInputElement | null }
  onCsvFileChange: (file: File | null) => void
  canImportCsv: boolean
  onImportCsvSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
}

export function InvestmentSettingsTab({
  accounts,
  accountTypeOptions,
  accountCurrencyOptions,
  selectedAccountId,
  selectedAccount,
  isCurrencyValid,
  createAccountStatus,
  createAccountErrorMessage,
  newAccountName,
  newAccountCurrency,
  newAccountType,
  onNewAccountNameChange,
  onNewAccountCurrencyChange,
  onNewAccountTypeChange,
  canCreate,
  onCreateAccountSubmit,
  onSelectAccount,
  isEditingSelectedAccount,
  editingAccountName,
  editingAccountType,
  onEditingAccountNameChange,
  onEditingAccountTypeChange,
  canUpdateAccount,
  updateAccountStatus,
  updateAccountErrorMessage,
  onStartEditingAccount,
  onUpdateAccountSubmit,
  onCancelEditingAccount,
  canDeleteAccount,
  deleteAccountStatus,
  deleteAccountErrorMessage,
  deleteAccountErrorAccountId,
  isDeleteAvailabilityKnown,
  hasLoadedTransactions,
  onDeleteAccount,
  onOpenTransactions,
  csvImportStatus,
  csvImportErrorMessage,
  csvImportResult,
  csvFile,
  csvFileInputRef,
  onCsvFileChange,
  canImportCsv,
  onImportCsvSubmit,
}: InvestmentSettingsTabProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.3fr)]">
      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <h2 className="text-sm font-semibold text-slate-900">
          Счета и создание
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Добавьте новый счет или выберите существующий для редактирования.
        </p>

        <form
          className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
          onSubmit={(event) => {
            void onCreateAccountSubmit(event)
          }}
        >
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Добавить счет
          </p>

          <div className="mt-3 space-y-2">
            <input
              type="text"
              value={newAccountName}
              onChange={(event) => onNewAccountNameChange(event.target.value)}
              placeholder="Название счета"
              className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
            />

            <div className="grid gap-2 sm:grid-cols-2">
              <select
                value={newAccountCurrency}
                onChange={(event) =>
                  onNewAccountCurrencyChange(event.target.value)
                }
                className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
              >
                {accountCurrencyOptions.map((currencyCode) => (
                  <option key={currencyCode} value={currencyCode}>
                    {currencyCode}
                  </option>
                ))}
              </select>

              <select
                value={newAccountType}
                onChange={(event) =>
                  onNewAccountTypeChange(event.target.value as AccountType)
                }
                className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
              >
                {accountTypeOptions.map((type) => (
                  <option key={type} value={type}>
                    {toAccountTypeLabel(type)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {!isCurrencyValid ? (
            <p className="mt-2 text-xs text-amber-700">
              Выберите валюту из списка.
            </p>
          ) : null}

          {createAccountStatus === 'error' && createAccountErrorMessage ? (
            <p className="mt-2 text-xs text-amber-700">
              {createAccountErrorMessage}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={!canCreate}
            className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {createAccountStatus === 'submitting' ? 'Добавляем...' : 'Добавить'}
          </button>
        </form>

        <InvestmentAccountSelectorList
          accounts={accounts}
          selectedAccountId={selectedAccountId}
          onSelectAccount={onSelectAccount}
          emptyMessage="Пока нет счетов. Создайте первый в форме выше."
        />
      </article>

      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        {!selectedAccount ? (
          <StatusCard
            tone="neutral"
            message="Выберите счет, чтобы изменить его настройки."
          />
        ) : (
          <>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">
                  {selectedAccount.name}
                </h2>
                <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                  {toAccountTypeLabel(selectedAccount.type)} ·{' '}
                  {selectedAccount.balance.currency}
                </p>
              </div>

              {!isEditingSelectedAccount ? (
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={onOpenTransactions}
                    className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800"
                  >
                    К транзакциям
                  </button>
                  <button
                    type="button"
                    onClick={onStartEditingAccount}
                    disabled={deleteAccountStatus === 'submitting'}
                    className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Редактировать счет
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      void onDeleteAccount()
                    }}
                    disabled={!canDeleteAccount}
                    className="rounded-md border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-medium text-rose-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {deleteAccountStatus === 'submitting'
                      ? 'Удаляем счет...'
                      : 'Удалить счет'}
                  </button>
                </div>
              ) : null}
            </div>

            {isEditingSelectedAccount ? (
              <form
                className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
                onSubmit={(event) => {
                  void onUpdateAccountSubmit(event)
                }}
              >
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  Редактировать счет
                </p>

                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  <label className="text-xs text-slate-600">
                    Название
                    <input
                      type="text"
                      value={editingAccountName}
                      onChange={(event) =>
                        onEditingAccountNameChange(event.target.value)
                      }
                      className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                    />
                  </label>

                  <label className="text-xs text-slate-600">
                    Тип
                    <select
                      value={editingAccountType}
                      onChange={(event) =>
                        onEditingAccountTypeChange(
                          event.target.value as AccountType,
                        )
                      }
                      className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
                    >
                      {accountTypeOptions.map((type) => (
                        <option key={type} value={type}>
                          {toAccountTypeLabel(type)}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <p className="mt-2 text-xs text-slate-500">
                  Валюта счета фиксируется при создании и не редактируется:{' '}
                  {selectedAccount.currency}.
                </p>

                {updateAccountStatus === 'error' &&
                updateAccountErrorMessage ? (
                  <p className="mt-2 text-xs text-amber-700">
                    {updateAccountErrorMessage}
                  </p>
                ) : null}

                <div className="mt-3 flex flex-wrap gap-2">
                  <button
                    type="submit"
                    disabled={!canUpdateAccount}
                    className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {updateAccountStatus === 'submitting'
                      ? 'Сохраняем...'
                      : 'Сохранить'}
                  </button>
                  <button
                    type="button"
                    onClick={onCancelEditingAccount}
                    className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600"
                  >
                    Отмена
                  </button>
                </div>
              </form>
            ) : (
              <p className="mt-2 text-xs text-slate-500">
                Валюта счета фиксируется при создании и не редактируется.
              </p>
            )}

            {!isEditingSelectedAccount && !isDeleteAvailabilityKnown ? (
              <p className="mt-2 text-xs text-slate-500">
                Удаление станет доступно после загрузки транзакций счета.
              </p>
            ) : null}

            {!isEditingSelectedAccount && hasLoadedTransactions ? (
              <p className="mt-2 text-xs text-slate-500">
                Удаление доступно только для пустого счета без транзакций.
              </p>
            ) : null}

            {!isEditingSelectedAccount &&
            deleteAccountStatus === 'error' &&
            deleteAccountErrorMessage &&
            deleteAccountErrorAccountId === selectedAccount.id ? (
              <p className="mt-2 text-xs text-amber-700">
                {deleteAccountErrorMessage}
              </p>
            ) : null}

            <form
              className="mt-4 rounded-lg border border-slate-200 bg-white p-3"
              onSubmit={(event) => {
                void onImportCsvSubmit(event)
              }}
            >
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Импорт из CSV
              </p>
              <p className="mt-1 text-xs text-slate-500">
                Счет для импорта: {selectedAccount.name}. Формат заголовка:{' '}
                <code>occurred_on,direction,amount,currency,memo</code>
              </p>

              <label className="mt-3 block text-xs text-slate-600">
                Файл
                <input
                  ref={csvFileInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  onChange={(event) =>
                    onCsvFileChange(event.target.files?.[0] ?? null)
                  }
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-2 file:py-1 file:text-xs file:font-medium file:text-slate-700"
                />
              </label>

              {csvFile ? (
                <p className="mt-2 text-xs text-slate-500">
                  Выбран файл:{' '}
                  <span className="font-medium text-slate-700">
                    {csvFile.name}
                  </span>
                </p>
              ) : null}

              {csvImportStatus === 'error' && csvImportErrorMessage ? (
                <p className="mt-2 text-xs text-amber-700">
                  {csvImportErrorMessage}
                </p>
              ) : null}

              {csvImportStatus === 'success' && csvImportResult ? (
                <p className="mt-2 text-xs text-emerald-700">
                  Импорт завершен: получено {csvImportResult.receivedRows},
                  добавлено {csvImportResult.importedCount}, пропущено
                  дубликатов {csvImportResult.skippedDuplicates}.
                </p>
              ) : null}

              <button
                type="submit"
                disabled={!canImportCsv}
                className="mt-3 rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {csvImportStatus === 'submitting'
                  ? 'Импортируем...'
                  : 'Импортировать CSV'}
              </button>
            </form>
          </>
        )}
      </article>
    </div>
  )
}
