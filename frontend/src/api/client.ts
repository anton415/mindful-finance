import { createHttpClient, type HttpClientConfig } from './http'
import type {
  AccountDto,
  CreateAccountRequest,
  CreateAccountResponse,
  CreateTransactionRequest,
  CreateTransactionResponse,
  CurrencyTotalsDto,
  ImportTransactionsCsvResponse,
  IsoDate,
  MoneyDto,
  TransactionDto,
} from './types'

export interface ApiClient {
  createAccount(request: CreateAccountRequest, signal?: AbortSignal): Promise<CreateAccountResponse>
  createTransaction(
    accountId: string,
    request: CreateTransactionRequest,
    signal?: AbortSignal,
  ): Promise<CreateTransactionResponse>
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
  }
}

export const apiClient = createApiClient()

function toEncodedAccountId(accountId: string): string {
  return encodeURIComponent(toRequiredAccountId(accountId))
}

function toRequiredAccountId(accountId: string): string {
  const trimmed = accountId.trim()
  if (trimmed.length === 0) {
    throw new Error('accountId must not be blank')
  }
  return trimmed
}
