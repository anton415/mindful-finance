interface ParsedMoneyInput {
  integerPart: string
  fractionPart: string
  hasDecimalSeparator: boolean
  hasValue: boolean
}

export function formatMoneyInput(value: string): string {
  const parsed = parseMoneyInput(value)
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
  if (!parsed.hasValue) {
    return ''
  }

  if (parsed.hasDecimalSeparator) {
    return `${parsed.integerPart}.${parsed.fractionPart}`
  }

  return parsed.integerPart
}

function parseMoneyInput(value: string): ParsedMoneyInput {
  const source = value.trim().replace(/\s+/g, '').replace(/,/g, '.')

  let integerDigits = ''
  let fractionDigits = ''
  let hasDecimalSeparator = false

  for (const symbol of source) {
    if (symbol >= '0' && symbol <= '9') {
      if (hasDecimalSeparator) {
        if (fractionDigits.length < 2) {
          fractionDigits += symbol
        }
      } else {
        integerDigits += symbol
      }
      continue
    }

    if (symbol === '.' && !hasDecimalSeparator) {
      hasDecimalSeparator = true
    }
  }

  if (integerDigits.length === 0 && (hasDecimalSeparator || fractionDigits.length > 0)) {
    integerDigits = '0'
  }

  integerDigits = integerDigits.replace(/^0+(?=\d)/, '')

  return {
    integerPart: integerDigits,
    fractionPart: fractionDigits,
    hasDecimalSeparator,
    hasValue: integerDigits.length > 0 || fractionDigits.length > 0 || hasDecimalSeparator,
  }
}
