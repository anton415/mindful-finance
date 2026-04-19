import type { DecimalAmount, TransactionDirection, TransactionDto } from './types'

export class ApiDecodeError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiDecodeError'
  }
}

export function decodeTransactionDtoList(
  input: unknown,
  source: string,
): TransactionDto[] {
  if (!Array.isArray(input)) {
    throw new ApiDecodeError(`${source} must return an array of transactions`)
  }

  return input.map((value, index) =>
    decodeTransactionDto(value, `${source}[${index}]`),
  )
}

function decodeTransactionDto(value: unknown, path: string): TransactionDto {
  const object = requireRecord(value, path)

  return {
    id: requireString(object, 'id', path),
    accountId: requireString(object, 'accountId', path),
    accountName: requireString(object, 'accountName', path),
    occurredOn: requireString(object, 'occurredOn', path),
    direction: requireDirection(object.direction, `${path}.direction`),
    amount: requireDecimalAmount(object.amount, `${path}.amount`),
    currency: requireString(object, 'currency', path),
    memo: requireNullableString(object, 'memo', path),
    instrumentSymbol: requireNullableString(object, 'instrumentSymbol', path),
    quantity: requireNullableDecimalAmount(object.quantity, `${path}.quantity`),
    unitPrice: requireNullableDecimalAmount(object.unitPrice, `${path}.unitPrice`),
    feeAmount: requireNullableDecimalAmount(object.feeAmount, `${path}.feeAmount`),
  }
}

function requireRecord(
  value: unknown,
  path: string,
): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new ApiDecodeError(`${path} must be an object`)
  }

  return value as Record<string, unknown>
}

function requireString(
  object: Record<string, unknown>,
  key: string,
  path: string,
): string {
  const value = object[key]
  if (typeof value !== 'string') {
    throw new ApiDecodeError(`${path}.${key} must be a string`)
  }

  return value
}

function requireNullableString(
  object: Record<string, unknown>,
  key: string,
  path: string,
): string | null {
  const value = object[key]
  if (value === null) {
    return null
  }
  if (typeof value !== 'string') {
    throw new ApiDecodeError(`${path}.${key} must be a string or null`)
  }

  return value
}

function requireDirection(
  value: unknown,
  path: string,
): TransactionDirection {
  if (value === 'INFLOW' || value === 'OUTFLOW') {
    return value
  }

  throw new ApiDecodeError(`${path} must be INFLOW or OUTFLOW`)
}

function requireDecimalAmount(
  value: unknown,
  path: string,
): DecimalAmount {
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }

  throw new ApiDecodeError(`${path} must be a decimal string or number`)
}

function requireNullableDecimalAmount(
  value: unknown,
  path: string,
): DecimalAmount | null {
  if (value === null) {
    return null
  }

  return requireDecimalAmount(value, path)
}
