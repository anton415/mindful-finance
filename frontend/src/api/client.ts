import { createHttpClient, type HttpClientConfig } from './http'
import type {
  AccountDto,
  CreatePersonalFinanceCardRequest,
  CreatePersonalFinanceCardResponse,
  CreateAccountRequest,
  CreateAccountResponse,
  CreateTransactionRequest,
  CreateTransactionResponse,
  CurrencyTotalsDto,
  ImportTransactionsCsvResponse,
  IsoDate,
  MoneyDto,
  PersonalFinanceCardDto,
  PersonalFinanceSnapshotDto,
  TransactionDto,
  UpdateIncomeForecastRequest,
  UpdateMonthlyExpenseRequest,
  UpdateMonthlyIncomeActualRequest,
  UpdateTransactionRequest,
} from './types'

export interface ApiClient {
  createAccount(request: CreateAccountRequest, signal?: AbortSignal): Promise<CreateAccountResponse>
  createTransaction(
    accountId: string,
    request: CreateTransactionRequest,
    signal?: AbortSignal,
  ): Promise<CreateTransactionResponse>
  updateTransaction(
    accountId: string,
    transactionId: string,
    request: UpdateTransactionRequest,
    signal?: AbortSignal,
  ): Promise<void>
  importTransactionsCsv(
    accountId: string,
    file: File,
    signal?: AbortSignal,
  ): Promise<ImportTransactionsCsvResponse>
  listAccounts(signal?: AbortSignal): Promise<AccountDto[]>
  listAccountTransactions(accountId: string, signal?: AbortSignal): Promise<TransactionDto[]>
  getAccountBalance(accountId: string, signal?: AbortSignal): Promise<MoneyDto>
  getNetWorth(signal?: AbortSignal): Promise<CurrencyTotalsDto>
  getMonthlyBurn(options?: { asOf?: IsoDate; signal?: AbortSignal }): Promise<CurrencyTotalsDto>
  getMonthlySavings(options?: { asOf?: IsoDate; signal?: AbortSignal }): Promise<CurrencyTotalsDto>
  listPersonalFinanceCards(signal?: AbortSignal): Promise<PersonalFinanceCardDto[]>
  createPersonalFinanceCard(
    request: CreatePersonalFinanceCardRequest,
    signal?: AbortSignal,
  ): Promise<CreatePersonalFinanceCardResponse>
  getPersonalFinanceSnapshot(
    cardId: string,
    year: number,
    signal?: AbortSignal,
  ): Promise<PersonalFinanceSnapshotDto>
  updateMonthlyExpenseActual(
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
    signal?: AbortSignal,
  ): Promise<void>
  updateMonthlyExpenseLimit(
    cardId: string,
    month: number,
    request: UpdateMonthlyExpenseRequest,
    signal?: AbortSignal,
  ): Promise<void>
  updateMonthlyIncomeActual(
    cardId: string,
    month: number,
    request: UpdateMonthlyIncomeActualRequest,
    signal?: AbortSignal,
  ): Promise<void>
  updateIncomeForecast(
    cardId: string,
    year: number,
    request: UpdateIncomeForecastRequest,
    signal?: AbortSignal,
  ): Promise<void>
}

export function createApiClient(config: HttpClientConfig = {}): ApiClient {
  const http = createHttpClient(config)

  return {
    createAccount(request: CreateAccountRequest, signal?: AbortSignal): Promise<CreateAccountResponse> {
      return http.postJson<CreateAccountResponse, CreateAccountRequest>('/accounts', request, { signal })
    },

    createTransaction(
      accountId: string,
      request: CreateTransactionRequest,
      signal?: AbortSignal,
    ): Promise<CreateTransactionResponse> {
      return http.postJson<CreateTransactionResponse, CreateTransactionRequest>(
        `/accounts/${toEncodedAccountId(accountId)}/transactions`,
        request,
        { signal },
      )
    },

    updateTransaction(
      accountId: string,
      transactionId: string,
      request: UpdateTransactionRequest,
      signal?: AbortSignal,
    ): Promise<void> {
      return http.putJson<void, UpdateTransactionRequest>(
        `/accounts/${toEncodedAccountId(accountId)}/transactions/${toEncodedTransactionId(transactionId)}`,
        request,
        { signal },
      )
    },

    importTransactionsCsv(
      accountId: string,
      file: File,
      signal?: AbortSignal,
    ): Promise<ImportTransactionsCsvResponse> {
      const formData = new FormData()
      formData.append('accountId', toRequiredAccountId(accountId))
      formData.append('file', file)

      return http.postFormData<ImportTransactionsCsvResponse>('/imports/transactions/csv', formData, {
        signal,
      })
    },

    listAccounts(signal?: AbortSignal): Promise<AccountDto[]> {
      return http.getJson<AccountDto[]>('/accounts', { signal })
    },

    listAccountTransactions(accountId: string, signal?: AbortSignal): Promise<TransactionDto[]> {
      return http.getJson<TransactionDto[]>(`/accounts/${toEncodedAccountId(accountId)}/transactions`, {
        signal,
      })
    },

    getAccountBalance(accountId: string, signal?: AbortSignal): Promise<MoneyDto> {
      return http.getJson<MoneyDto>(`/accounts/${toEncodedAccountId(accountId)}/balance`, { signal })
    },

    getNetWorth(signal?: AbortSignal): Promise<CurrencyTotalsDto> {
      return http.getJson<CurrencyTotalsDto>('/net-worth', { signal })
    },

    getMonthlyBurn(options?: { asOf?: IsoDate; signal?: AbortSignal }): Promise<CurrencyTotalsDto> {
      const query = options?.asOf ? { asOf: options.asOf } : undefined
      return http.getJson<CurrencyTotalsDto>('/peace/monthly-burn', {
        query,
        signal: options?.signal,
      })
    },

    getMonthlySavings(options?: { asOf?: IsoDate; signal?: AbortSignal }): Promise<CurrencyTotalsDto> {
      const query = options?.asOf ? { asOf: options.asOf } : undefined
      return http.getJson<CurrencyTotalsDto>('/peace/monthly-savings', {
        query,
        signal: options?.signal,
      })
    },

    listPersonalFinanceCards(signal?: AbortSignal): Promise<PersonalFinanceCardDto[]> {
      return http.getJson<PersonalFinanceCardDto[]>('/personal-finance/cards', { signal })
    },

    createPersonalFinanceCard(
      request: CreatePersonalFinanceCardRequest,
      signal?: AbortSignal,
    ): Promise<CreatePersonalFinanceCardResponse> {
      return http.postJson<CreatePersonalFinanceCardResponse, CreatePersonalFinanceCardRequest>(
        '/personal-finance/cards',
        request,
        { signal },
      )
    },

    getPersonalFinanceSnapshot(
      cardId: string,
      year: number,
      signal?: AbortSignal,
    ): Promise<PersonalFinanceSnapshotDto> {
      return http.getJson<PersonalFinanceSnapshotDto>(
        `/personal-finance/cards/${toEncodedPersonalFinanceCardId(cardId)}/years/${toRequiredYear(year)}`,
        { signal },
      )
    },

    updateMonthlyExpenseActual(
      cardId: string,
      month: number,
      request: UpdateMonthlyExpenseRequest,
      signal?: AbortSignal,
    ): Promise<void> {
      return http.putJson<void, UpdateMonthlyExpenseRequest>(
        `/personal-finance/cards/${toEncodedPersonalFinanceCardId(cardId)}/expenses/actual/${toRequiredMonth(month)}`,
        request,
        { signal },
      )
    },

    updateMonthlyExpenseLimit(
      cardId: string,
      month: number,
      request: UpdateMonthlyExpenseRequest,
      signal?: AbortSignal,
    ): Promise<void> {
      return http.putJson<void, UpdateMonthlyExpenseRequest>(
        `/personal-finance/cards/${toEncodedPersonalFinanceCardId(cardId)}/expenses/limits/${toRequiredMonth(month)}`,
        request,
        { signal },
      )
    },

    updateMonthlyIncomeActual(
      cardId: string,
      month: number,
      request: UpdateMonthlyIncomeActualRequest,
      signal?: AbortSignal,
    ): Promise<void> {
      return http.putJson<void, UpdateMonthlyIncomeActualRequest>(
        `/personal-finance/cards/${toEncodedPersonalFinanceCardId(cardId)}/income/actual/${toRequiredMonth(month)}`,
        request,
        { signal },
      )
    },

    updateIncomeForecast(
      cardId: string,
      year: number,
      request: UpdateIncomeForecastRequest,
      signal?: AbortSignal,
    ): Promise<void> {
      return http.putJson<void, UpdateIncomeForecastRequest>(
        `/personal-finance/cards/${toEncodedPersonalFinanceCardId(cardId)}/income/forecast/${toRequiredYear(year)}`,
        request,
        { signal },
      )
    },
  }
}

export const apiClient = createApiClient()

function toEncodedAccountId(accountId: string): string {
  return encodeURIComponent(toRequiredIdentifier(accountId, 'accountId'))
}

function toEncodedTransactionId(transactionId: string): string {
  return encodeURIComponent(toRequiredIdentifier(transactionId, 'transactionId'))
}

function toEncodedPersonalFinanceCardId(cardId: string): string {
  return encodeURIComponent(toRequiredIdentifier(cardId, 'cardId'))
}

function toRequiredAccountId(accountId: string): string {
  return toRequiredIdentifier(accountId, 'accountId')
}

function toRequiredYear(year: number): string {
  if (!Number.isInteger(year) || year < 1 || year > 9999) {
    throw new Error('year must be an integer between 1 and 9999')
  }

  return String(year)
}

function toRequiredMonth(month: number): string {
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    throw new Error('month must be an integer between 1 and 12')
  }

  return String(month)
}

function toRequiredIdentifier(value: string, fieldName: string): string {
  const trimmed = value.trim()
  if (trimmed.length === 0) {
    throw new Error(`${fieldName} must not be blank`)
  }
  return trimmed
}
