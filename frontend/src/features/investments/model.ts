import type { AccountType, CurrencyTotalsDto } from '../../api'
import { addDecimalAmountsExact, toAccountTypeLabel } from './formatting'
import type { AccountWithBalance, InvestmentTab, TabCopy } from './types'

export const INVESTMENT_TAB_COPY: Record<InvestmentTab, TabCopy> = {
  overview: {
    title: 'Структура портфеля',
    description:
      'Смотрите базовые метрики по счетам, валютам и типам без скрытой FX-конвертации.',
  },
  transactions: {
    title: 'Покупки и продажи',
    description:
      'Выберите счет и зафиксируйте движение денег по инвестиционным операциям, импорту и ручным правкам.',
  },
  settings: {
    title: 'Настройки счетов',
    description:
      'Добавляйте новые инвестиционные счета, редактируйте существующие и удаляйте пустые счета.',
  },
}

export interface InvestmentOverviewModel {
  totalAccountCount: number
  activeAccountCount: number
  archivedAccountCount: number
  currencyCount: number
  totalsByCurrency: CurrencyTotalsDto
  typeBreakdown: Array<{
    type: AccountType
    accountCount: number
    totalsByCurrency: CurrencyTotalsDto
  }>
}

export function buildInvestmentOverview(
  accounts: AccountWithBalance[],
): InvestmentOverviewModel {
  const totalsByCurrency: CurrencyTotalsDto = {}
  const typeBreakdownMap = new Map<
    AccountType,
    { accountCount: number; totalsByCurrency: CurrencyTotalsDto }
  >()

  for (const account of accounts) {
    totalsByCurrency[account.balance.currency] = addDecimalAmountsExact(
      totalsByCurrency[account.balance.currency] ?? '0.00',
      account.balance.amount,
    )

    const summary = typeBreakdownMap.get(account.type) ?? {
      accountCount: 0,
      totalsByCurrency: {},
    }
    summary.accountCount += 1
    summary.totalsByCurrency[account.balance.currency] = addDecimalAmountsExact(
      summary.totalsByCurrency[account.balance.currency] ?? '0.00',
      account.balance.amount,
    )
    typeBreakdownMap.set(account.type, summary)
  }

  const typeBreakdown = [...typeBreakdownMap.entries()]
    .map(([type, summary]) => ({
      type,
      accountCount: summary.accountCount,
      totalsByCurrency: summary.totalsByCurrency,
    }))
    .sort((left, right) =>
      toAccountTypeLabel(left.type).localeCompare(
        toAccountTypeLabel(right.type),
      ),
    )

  return {
    totalAccountCount: accounts.length,
    activeAccountCount: accounts.filter(
      (account) => account.status === 'ACTIVE',
    ).length,
    archivedAccountCount: accounts.filter(
      (account) => account.status === 'ARCHIVED',
    ).length,
    currencyCount: Object.keys(totalsByCurrency).length,
    totalsByCurrency,
    typeBreakdown,
  }
}
