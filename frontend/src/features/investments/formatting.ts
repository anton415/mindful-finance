import type { AccountType, TransactionDirection } from '../../api'
import type { AccountStatus } from './types'

export function addDecimalAmountsExact(left: string, right: string): string {
  return fromDecimalCents(toDecimalCents(left) + toDecimalCents(right))
}

export function formatAmount(amount: string): string {
  const isNegative = amount.startsWith('-')
  const unsigned = isNegative ? amount.slice(1) : amount
  const [integerRaw, fractionRaw = '00'] = unsigned.split('.')
  const integerPart = integerRaw.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  const fractionPart = fractionRaw.padEnd(2, '0').slice(0, 2)
  return `${isNegative ? '-' : ''}${integerPart},${fractionPart}`
}

export function formatSignedAmount(
  amount: string,
  direction: 'INFLOW' | 'OUTFLOW',
): string {
  const prefix = direction === 'OUTFLOW' ? '-' : '+'
  return `${prefix}${formatAmount(amount)}`
}

export function isValidIsoDateValue(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(value)
}

export function isValidPositiveAmountValue(value: string): boolean {
  return /^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(value) && Number(value) > 0
}

export function normalizeTransactionMemo(memo: string | null): string {
  return memo ?? ''
}

export function toTransactionMemoDisplay(memo: string | null): string {
  const normalizedMemo = normalizeTransactionMemo(memo)
  return normalizedMemo.length > 0 ? normalizedMemo : 'Без описания'
}

export function todayIsoDate(): string {
  const now = new Date()
  const year = String(now.getFullYear())
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function formatIsoDateRu(isoDate: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) {
    return isoDate
  }

  const [year, month, day] = isoDate.split('-')
  return `${day}.${month}.${year}`
}

export function toDirectionLabel(direction: TransactionDirection): string {
  return toDirectionSelectLabel(direction)
}

export function toDirectionSelectLabel(
  direction: TransactionDirection,
): string {
  return direction === 'INFLOW' ? 'Доход' : 'Расход'
}

export function toAccountTypeLabel(type: AccountType): string {
  const labels: Record<AccountType, string> = {
    CASH: 'Наличные',
    DEPOSIT: 'Депозит',
    FUND: 'Фонд',
    IIS: 'ИИС',
    BROKERAGE: 'Брокерский',
  }
  return labels[type]
}

export function toAccountStatusLabel(status: AccountStatus): string {
  if (status === 'ACTIVE') {
    return 'Активный'
  }
  if (status === 'ARCHIVED') {
    return 'Архивный'
  }
  return status
}

export function toInvestmentAccountCountLabel(count: number): string {
  const remainder10 = count % 10
  const remainder100 = count % 100

  if (remainder10 === 1 && remainder100 !== 11) {
    return 'счет'
  }
  if (
    remainder10 >= 2 &&
    remainder10 <= 4 &&
    (remainder100 < 12 || remainder100 > 14)
  ) {
    return 'счета'
  }
  return 'счетов'
}

function toDecimalCents(value: string): bigint {
  const normalized = value.trim()
  const isNegative = normalized.startsWith('-')
  const unsigned = isNegative ? normalized.slice(1) : normalized
  const [integerPartRaw, fractionPartRaw = ''] = unsigned.split('.')
  const integerPart = integerPartRaw.length > 0 ? integerPartRaw : '0'
  const fractionPart = fractionPartRaw.padEnd(2, '0').slice(0, 2)
  const cents =
    BigInt(integerPart) * 100n +
    BigInt(fractionPart.length > 0 ? fractionPart : '0')

  return isNegative ? -cents : cents
}

function fromDecimalCents(value: bigint): string {
  const isNegative = value < 0n
  const unsigned = isNegative ? -value : value
  const integerPart = unsigned / 100n
  const fractionPart = String(unsigned % 100n).padStart(2, '0')

  return `${isNegative ? '-' : ''}${integerPart}.${fractionPart}`
}
