import type {
  CreateTransactionRequest,
  DecimalAmount,
  TransactionDirection,
  TransactionDto,
  UpdateTransactionRequest,
} from '../../api'
import { formatMoneyInput, normalizeMoneyInput } from '../../money-input'
import { isValidIsoDateValue, normalizeTransactionMemo, todayIsoDate } from './formatting'

export interface TransactionTradeDraft {
  accountId: string
  occurredOn: string
  direction: TransactionDirection
  instrumentSymbol: string
  unitPrice: string
  quantity: string
  feeAmount: string
  memo: string
}

export interface TransactionTradeEvaluation {
  occurredOn: string
  direction: TransactionDirection
  instrumentSymbol: string
  unitPrice: DecimalAmount
  quantity: DecimalAmount
  feeAmount: DecimalAmount
  memo: string
  totalAmount: DecimalAmount | null
  isDateValid: boolean
  isInstrumentSymbolValid: boolean
  isUnitPriceValid: boolean
  isQuantityValid: boolean
  isFeeAmountValid: boolean
  isTotalAmountValid: boolean
}

const DEFAULT_DRAFT: TransactionTradeDraft = {
  accountId: '',
  occurredOn: todayIsoDate(),
  direction: 'OUTFLOW',
  instrumentSymbol: '',
  unitPrice: '',
  quantity: '1',
  feeAmount: '0',
  memo: '',
}

export function createEmptyTransactionTradeDraft(
  accountId: string = '',
): TransactionTradeDraft {
  return {
    ...DEFAULT_DRAFT,
    accountId,
    occurredOn: todayIsoDate(),
  }
}

export function createTransactionTradeDraftFromTransaction(
  transaction: TransactionDto,
): TransactionTradeDraft {
  return {
    accountId: transaction.accountId,
    occurredOn: transaction.occurredOn,
    direction: transaction.direction,
    instrumentSymbol: transaction.instrumentSymbol ?? '',
    unitPrice: formatMoneyInput(transaction.unitPrice ?? transaction.amount),
    quantity: formatDecimalInput(transaction.quantity ?? '1', 6),
    feeAmount: formatMoneyInput(transaction.feeAmount ?? '0'),
    memo: normalizeTransactionMemo(transaction.memo),
  }
}

export function evaluateTransactionTradeDraft(
  draft: TransactionTradeDraft,
): TransactionTradeEvaluation {
  const occurredOn = draft.occurredOn.trim()
  const instrumentSymbol = normalizeInstrumentSymbol(draft.instrumentSymbol)
  const unitPrice = normalizeMoneyInput(draft.unitPrice)
  const quantity = normalizeDecimalInput(draft.quantity, 6)
  const feeAmountCandidate = normalizeMoneyInput(draft.feeAmount)
  const feeAmount = feeAmountCandidate.length > 0 ? feeAmountCandidate : '0'
  const memo = draft.memo.trim()
  const totalAmount =
    isValidIsoDateValue(occurredOn) &&
    instrumentSymbol.length > 0 &&
    isValidPositiveAmountValue(unitPrice, 2) &&
    isValidPositiveAmountValue(quantity, 6) &&
    isValidNonNegativeAmountValue(feeAmount, 2)
      ? computeTradeTotalAmount({
          direction: draft.direction,
          unitPrice,
          quantity,
          feeAmount,
        })
      : null

  return {
    occurredOn,
    direction: draft.direction,
    instrumentSymbol,
    unitPrice,
    quantity,
    feeAmount,
    memo,
    totalAmount,
    isDateValid: isValidIsoDateValue(occurredOn),
    isInstrumentSymbolValid: instrumentSymbol.length > 0,
    isUnitPriceValid: isValidPositiveAmountValue(unitPrice, 2),
    isQuantityValid: isValidPositiveAmountValue(quantity, 6),
    isFeeAmountValid: isValidNonNegativeAmountValue(feeAmount, 2),
    isTotalAmountValid: totalAmount !== null,
  }
}

export function toCreateTransactionTradeRequest(
  evaluation: TransactionTradeEvaluation,
): CreateTransactionRequest | null {
  if (!isTransactionTradeEvaluationValid(evaluation)) {
    return null
  }

  return {
    occurredOn: evaluation.occurredOn,
    direction: evaluation.direction,
    amount: evaluation.totalAmount ?? undefined,
    memo: evaluation.memo,
    instrumentSymbol: evaluation.instrumentSymbol,
    quantity: evaluation.quantity,
    unitPrice: evaluation.unitPrice,
    feeAmount: evaluation.feeAmount,
  }
}

export function toUpdateTransactionTradeRequest(
  evaluation: TransactionTradeEvaluation,
): UpdateTransactionRequest | null {
  if (!isTransactionTradeEvaluationValid(evaluation)) {
    return null
  }

  return {
    occurredOn: evaluation.occurredOn,
    direction: evaluation.direction,
    amount: evaluation.totalAmount ?? undefined,
    memo: evaluation.memo,
    instrumentSymbol: evaluation.instrumentSymbol,
    quantity: evaluation.quantity,
    unitPrice: evaluation.unitPrice,
    feeAmount: evaluation.feeAmount,
  }
}

export function canSubmitTransactionTrade(
  evaluation: TransactionTradeEvaluation,
): boolean {
  return isTransactionTradeEvaluationValid(evaluation)
}

export function formatDecimalInput(value: string, maxFractionDigits: number): string {
  const normalized = normalizeDecimalInput(value, maxFractionDigits)
  if (normalized.length === 0) {
    return ''
  }

  const [integerRaw, fractionRaw = ''] = normalized.split('.')
  const integerPart = integerRaw.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  return fractionRaw.length > 0 ? `${integerPart}.${fractionRaw}` : integerPart
}

export function normalizeDecimalInput(value: string, maxFractionDigits: number): string {
  const trimmed = value.trim()
  if (trimmed.length === 0) {
    return ''
  }

  const source = trimmed.replace(/\s+/g, '')
  if (/[+-]/.test(source) || /[^0-9.,]/.test(source)) {
    return value.trim()
  }

  const dotCount = countOccurrences(source, '.')
  const commaCount = countOccurrences(source, ',')
  if ((dotCount > 0 && commaCount > 0) || dotCount > 1 || commaCount > 1) {
    return value.trim()
  }

  const decimalSeparator = dotCount === 1 ? '.' : commaCount === 1 ? ',' : null
  const [integerRaw, fractionRaw = ''] =
    decimalSeparator === null ? [source, ''] : source.split(decimalSeparator)

  if (!/^\d*$/.test(integerRaw) || !/^\d*$/.test(fractionRaw)) {
    return value.trim()
  }

  const integerPart = integerRaw.replace(/^0+(?=\d)/, '') || '0'
  const fractionPart = fractionRaw.slice(0, maxFractionDigits)

  if (decimalSeparator === null) {
    return integerPart
  }

  return `${integerPart}.${fractionPart}`
}

export function normalizeInstrumentSymbol(value: string): string {
  return value.trim().toUpperCase()
}

export function computeTradeTotalAmount(input: {
  direction: TransactionDirection
  unitPrice: DecimalAmount
  quantity: DecimalAmount
  feeAmount: DecimalAmount
}): DecimalAmount | null {
  const unitPriceDecimal = parseDecimal(input.unitPrice)
  const quantityDecimal = parseDecimal(input.quantity)
  const feeAmountDecimal = parseDecimal(input.feeAmount)

  if (!unitPriceDecimal || !quantityDecimal || !feeAmountDecimal) {
    return null
  }

  const grossAmount = multiplyDecimals(unitPriceDecimal, quantityDecimal)
  const totalAmount =
    input.direction === 'OUTFLOW'
      ? addDecimals(grossAmount, feeAmountDecimal)
      : subtractDecimals(grossAmount, feeAmountDecimal)

  if (totalAmount.sign < 0) {
    return null
  }

  return toCurrencyAmountString(totalAmount)
}

export function toTradeDirectionLabel(direction: TransactionDirection): string {
  return direction === 'OUTFLOW' ? 'Купить' : 'Продать'
}

export function isBuyDirection(direction: TransactionDirection): boolean {
  return direction === 'OUTFLOW'
}

function isTransactionTradeEvaluationValid(
  evaluation: TransactionTradeEvaluation,
): boolean {
  return (
    evaluation.isDateValid &&
    evaluation.isInstrumentSymbolValid &&
    evaluation.isUnitPriceValid &&
    evaluation.isQuantityValid &&
    evaluation.isFeeAmountValid &&
    evaluation.isTotalAmountValid
  )
}

function isValidPositiveAmountValue(
  value: string,
  maxFractionDigits: number,
): boolean {
  return isValidAmountValue(value, maxFractionDigits, { allowZero: false })
}

function isValidNonNegativeAmountValue(
  value: string,
  maxFractionDigits: number,
): boolean {
  return isValidAmountValue(value, maxFractionDigits, { allowZero: true })
}

function isValidAmountValue(
  value: string,
  maxFractionDigits: number,
  options: { allowZero: boolean },
): boolean {
  if (!new RegExp(`^(?:0|[1-9]\\d*)(?:\\.\\d{1,${maxFractionDigits}})?$`).test(value)) {
    return false
  }

  if (options.allowZero) {
    return true
  }

  return parseDecimal(value)?.sign === 1
}

interface ParsedDecimal {
  sign: -1 | 0 | 1
  scale: number
  value: bigint
}

function parseDecimal(value: string): ParsedDecimal | null {
  const normalized = value.trim()
  if (!/^\d+(?:\.\d+)?$/.test(normalized)) {
    return null
  }

  const [integerPart, fractionPart = ''] = normalized.split('.')
  const digits = `${integerPart}${fractionPart}`
  const bigintValue = BigInt(digits)

  return {
    sign: bigintValue === 0n ? 0 : 1,
    scale: fractionPart.length,
    value: bigintValue,
  }
}

function multiplyDecimals(left: ParsedDecimal, right: ParsedDecimal): ParsedDecimal {
  const value = left.value * right.value
  return {
    sign: value === 0n ? 0 : 1,
    scale: left.scale + right.scale,
    value,
  }
}

function addDecimals(left: ParsedDecimal, right: ParsedDecimal): ParsedDecimal {
  const scale = Math.max(left.scale, right.scale)
  const value = alignDecimal(left, scale) + alignDecimal(right, scale)
  return {
    sign: value === 0n ? 0 : 1,
    scale,
    value,
  }
}

function subtractDecimals(left: ParsedDecimal, right: ParsedDecimal): ParsedDecimal {
  const scale = Math.max(left.scale, right.scale)
  const value = alignDecimal(left, scale) - alignDecimal(right, scale)
  return {
    sign: value === 0n ? 0 : value > 0n ? 1 : -1,
    scale,
    value,
  }
}

function alignDecimal(value: ParsedDecimal, scale: number): bigint {
  const multiplier = 10n ** BigInt(scale - value.scale)
  return value.value * multiplier
}

function toCurrencyAmountString(value: ParsedDecimal): DecimalAmount | null {
  const normalized = trimTrailingZeros(value.value, value.scale)
  if (normalized.scale > 2) {
    return null
  }

  const scale = 2
  const alignedValue = normalized.value * 10n ** BigInt(scale - normalized.scale)
  const integerPart = alignedValue / 100n
  const fractionPart = String(alignedValue % 100n).padStart(2, '0')
  return `${integerPart}.${fractionPart}`
}

function trimTrailingZeros(value: bigint, scale: number): ParsedDecimal {
  let currentValue = value
  let currentScale = scale

  while (currentScale > 0 && currentValue % 10n === 0n) {
    currentValue /= 10n
    currentScale -= 1
  }

  return {
    sign: currentValue === 0n ? 0 : 1,
    scale: currentScale,
    value: currentValue,
  }
}

function countOccurrences(value: string, symbol: string): number {
  return value.split(symbol).length - 1
}
