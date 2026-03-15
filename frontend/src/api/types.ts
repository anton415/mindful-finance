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

export type PersonalExpenseCategoryCode =
  | 'RESTAURANTS'
  | 'GROCERIES'
  | 'PERSONAL'
  | 'UTILITIES'
  | 'TRANSPORT'
  | 'GIFTS'
  | 'INVESTMENTS'
  | 'ENTERTAINMENT'
  | 'EDUCATION'

export type ExpenseLimitPeriod = 'MONTHLY' | 'ANNUAL'

export type PersonalFinanceCardStatus = 'ACTIVE' | 'ARCHIVED'

export interface PersonalExpenseCategoryDto {
  code: PersonalExpenseCategoryCode
  label: string
  limitPeriod: ExpenseLimitPeriod
}

export interface PersonalFinanceCardDto {
  id: string
  name: string
  createdAt: string
  status: PersonalFinanceCardStatus
}

export interface CreatePersonalFinanceCardRequest {
  name: string
}

export interface CreatePersonalFinanceCardResponse {
  cardId: string
}

export interface UpdatePersonalFinanceCardRequest {
  name: string
}

export interface PersonalFinanceExpenseMonthDto {
  month: number
  actualCategoryAmounts: Record<PersonalExpenseCategoryCode, DecimalAmount>
  limitCategoryAmounts: Record<PersonalExpenseCategoryCode, DecimalAmount>
  actualTotal: DecimalAmount
  limitTotal: DecimalAmount
}

export interface PersonalFinanceExpensesDto {
  months: PersonalFinanceExpenseMonthDto[]
  actualTotalsByCategory: Record<PersonalExpenseCategoryCode, DecimalAmount>
  limitTotalsByCategory: Record<PersonalExpenseCategoryCode, DecimalAmount>
  annualActualTotal: DecimalAmount
  annualLimitTotal: DecimalAmount
  averageMonthlyActualTotal: DecimalAmount
}

export type PersonalFinanceIncomeMonthStatus = 'ACTUAL' | 'FORECAST'

export interface PersonalFinanceIncomeMonthDto {
  month: number
  totalAmount: DecimalAmount
  status: PersonalFinanceIncomeMonthStatus | null
}

export interface PersonalFinanceIncomeForecastDto {
  salaryAmount: DecimalAmount
  bonusPercent: DecimalAmount
  bonusAmount: DecimalAmount
  totalAmount: DecimalAmount
}

export interface PersonalFinanceIncomeDto {
  months: PersonalFinanceIncomeMonthDto[]
  annualTotal: DecimalAmount
  averageMonthlyTotal: DecimalAmount
}

export interface PersonalFinanceSettingsDto {
  currentBalance: DecimalAmount
  baselineAmount: DecimalAmount
  limitCategoryAmounts: Record<PersonalExpenseCategoryCode, DecimalAmount>
  monthlyLimitTotal: DecimalAmount
  annualLimitTotal: DecimalAmount
  incomeForecast: PersonalFinanceIncomeForecastDto | null
}

export interface PersonalFinanceSnapshotDto {
  cards: PersonalFinanceCardDto[]
  card: PersonalFinanceCardDto
  year: number
  currency: CurrencyCode
  categories: PersonalExpenseCategoryDto[]
  expenses: PersonalFinanceExpensesDto
  income: PersonalFinanceIncomeDto
  settings: PersonalFinanceSettingsDto
}

export interface UpdateMonthlyExpenseRequest {
  year: number
  categoryAmounts: Record<PersonalExpenseCategoryCode, DecimalAmount>
}

export interface UpdateMonthlyIncomeActualRequest {
  year: number
  totalAmount: DecimalAmount
}

export interface UpdatePersonalFinanceSettingsRequest {
  baselineAmount: DecimalAmount
  limitCategoryAmounts: Record<PersonalExpenseCategoryCode, DecimalAmount>
  salaryAmount: DecimalAmount
  bonusPercent: DecimalAmount
}

export interface ApiErrorDto {
  error: string
  message: string
}
