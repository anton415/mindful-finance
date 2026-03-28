interface ParsedMoneyInput {
  integerPart: string
  fractionPart: string
  hasDecimalSeparator: boolean
  hasValue: boolean
  isValid: boolean
}

export function formatMoneyInput(value: string): string {
  const parsed = parseMoneyInput(value)
  if (!parsed.isValid) {
    return value.trim()
  }

  if (!parsed.hasValue) {
    return ''
  }

  const groupedInteger = parsed.integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  if (parsed.hasDecimalSeparator) {
    return `${groupedInteger}.${parsed.fractionPart}`
  }

  return groupedInteger
}

export function normalizeMoneyInput(value: string): string {
  const parsed = parseMoneyInput(value)
  if (!parsed.isValid) {
    return value.trim()
  }

  if (!parsed.hasValue) {
    return ''
  }

  if (parsed.hasDecimalSeparator) {
    return `${parsed.integerPart}.${parsed.fractionPart}`
  }

  return parsed.integerPart
}

function parseMoneyInput(value: string): ParsedMoneyInput {
  const trimmed = value.trim()
  if (trimmed.length === 0) {
    return {
      integerPart: '',
      fractionPart: '',
      hasDecimalSeparator: false,
      hasValue: false,
      isValid: true,
    }
  }

  const source = trimmed.replace(/\s+/g, '')
  if (/[+-]/.test(source) || /[^0-9.,]/.test(source)) {
    return invalidParsedMoneyInput
  }

  const dotCount = countOccurrences(source, '.')
  const commaCount = countOccurrences(source, ',')
  if ((dotCount > 0 && commaCount > 0) || dotCount > 1 || commaCount > 1) {
    return invalidParsedMoneyInput
  }

  const decimalSeparator = dotCount === 1 ? '.' : commaCount === 1 ? ',' : null
  const [integerRaw, fractionRaw = ''] = decimalSeparator === null ? [source, ''] : source.split(decimalSeparator)
  if (!/^\d*$/.test(integerRaw) || !/^\d*$/.test(fractionRaw)) {
    return invalidParsedMoneyInput
  }

  let integerDigits = integerRaw.replace(/^0+(?=\d)/, '')
  if (integerDigits.length === 0 && decimalSeparator !== null) {
    integerDigits = '0'
  }

  return {
    integerPart: integerDigits,
    fractionPart: fractionRaw,
    hasDecimalSeparator: decimalSeparator !== null,
    hasValue: integerDigits.length > 0 || fractionRaw.length > 0 || decimalSeparator !== null,
    isValid: true,
  }
}

const invalidParsedMoneyInput: ParsedMoneyInput = {
  integerPart: '',
  fractionPart: '',
  hasDecimalSeparator: false,
  hasValue: false,
  isValid: false,
}

function countOccurrences(value: string, symbol: string): number {
  return value.split(symbol).length - 1
}
