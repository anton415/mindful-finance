import type {
  AccountDto,
  AccountType,
  MoneyDto,
  TransactionDirection,
} from '../../api'

export interface AccountWithBalance extends AccountDto {
  balance: MoneyDto
}

export type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
export type TransactionDirectionFilter = 'ALL' | 'INFLOW' | 'OUTFLOW'
export type CreateAccountStatus = 'idle' | 'submitting' | 'error'
export type CreateTransactionStatus = 'idle' | 'submitting' | 'error'
export type CsvImportStatus = 'idle' | 'submitting' | 'success' | 'error'
export type InvestmentTab = 'overview' | 'transactions' | 'settings'
export type AccountStatus = AccountDto['status']
export type TabCopy = { title: string; description: string }
export type DirectionValue = TransactionDirection
export type AccountKind = AccountType
