export type DecimalAmount = string
export type CurrencyCode = string
export type IsoDate = string

export type AccountType = 'CASH' | 'DEPOSIT' | 'FUND' | 'IIS' | 'BROKERAGE'

export interface AccountDto {
  id: string
  name: string
  currency: CurrencyCode
  type: AccountType
  status: string
}

export interface CreateAccountRequest {
  name: string
  currency: CurrencyCode
  type: AccountType
}

export interface CreateAccountResponse {
  accountId: string
}

export type TransactionDirection = 'INFLOW' | 'OUTFLOW'

export interface CreateTransactionRequest {
  occurredOn: IsoDate
  direction: TransactionDirection
  amount: DecimalAmount
  memo: string
}

export interface UpdateTransactionRequest {
  occurredOn: IsoDate
  direction: TransactionDirection
  amount: DecimalAmount
  memo: string
}

export interface CreateTransactionResponse {
  transactionId: string
}

export interface ImportTransactionsCsvResponse {
  receivedRows: number
  importedCount: number
  skippedDuplicates: number
}

export interface TransactionDto {
  id: string
  occurredOn: IsoDate
  direction: TransactionDirection
  amount: DecimalAmount
  currency: CurrencyCode
  memo: string | null
}

export interface MoneyDto {
  amount: DecimalAmount
  currency: CurrencyCode
}

export type CurrencyTotalsDto = Record<CurrencyCode, DecimalAmount>

export interface ApiErrorDto {
  error: string
  message: string
}
