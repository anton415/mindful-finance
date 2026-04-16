import type { CurrencyTotalsDto } from '../../api'
import {
  formatAmount,
  toAccountStatusLabel,
  toAccountTypeLabel,
} from './formatting'
import type { AccountWithBalance } from './types'

interface MetricCardProps {
  title: string
  subtitle: string
  totals: CurrencyTotalsDto
}

export function MetricCard({ title, subtitle, totals }: MetricCardProps) {
  const entries = Object.entries(totals).sort(([left], [right]) =>
    left.localeCompare(right),
  )

  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
      <p className="mt-1 text-xs leading-relaxed text-slate-500">{subtitle}</p>

      {entries.length === 0 ? (
        <p className="mt-6 text-sm text-slate-600">Пока нет данных.</p>
      ) : (
        <ul className="mt-6 space-y-2">
          {entries.map(([currency, amount]) => (
            <li
              key={currency}
              className="flex items-center justify-between gap-3 text-sm"
            >
              <span className="font-medium text-slate-700">{currency}</span>
              <span className="font-semibold text-slate-900">
                {formatAmount(amount)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}

interface StatusCardProps {
  tone: 'neutral' | 'warning'
  message: string
  actionLabel?: string
  onAction?: () => void
}

export function StatusCard({
  tone,
  message,
  actionLabel,
  onAction,
}: StatusCardProps) {
  const toneClasses =
    tone === 'warning'
      ? 'border-amber-200 bg-amber-50 text-amber-900'
      : 'border-slate-200 bg-slate-50 text-slate-600'

  return (
    <div className={`rounded-xl border px-4 py-5 text-sm ${toneClasses}`}>
      <p>{message}</p>
      {actionLabel && onAction ? (
        <button
          type="button"
          onClick={onAction}
          className="mt-3 rounded-md border border-current px-3 py-1 text-xs font-medium"
        >
          {actionLabel}
        </button>
      ) : null}
    </div>
  )
}

interface InvestmentAccountSelectorListProps {
  accounts: AccountWithBalance[]
  selectedAccountId: string | null
  onSelectAccount: (accountId: string) => void
  emptyMessage: string
}

export function InvestmentAccountSelectorList({
  accounts,
  selectedAccountId,
  onSelectAccount,
  emptyMessage,
}: InvestmentAccountSelectorListProps) {
  if (accounts.length === 0) {
    return (
      <div className="mt-4">
        <StatusCard tone="neutral" message={emptyMessage} />
      </div>
    )
  }

  return (
    <ul className="mt-4 space-y-2">
      {accounts.map((account) => (
        <li key={account.id}>
          <button
            type="button"
            onClick={() => onSelectAccount(account.id)}
            className={`w-full rounded-lg border px-3 py-3 text-left ${
              selectedAccountId === account.id
                ? 'border-slate-300 bg-white shadow-sm'
                : 'border-slate-200 bg-transparent'
            }`}
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-slate-900">
                  {account.name}
                </p>
                <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                  {toAccountTypeLabel(account.type)} ·{' '}
                  {toAccountStatusLabel(account.status)}
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs uppercase tracking-wide text-slate-500">
                  {account.balance.currency}
                </p>
                <p className="mt-1 text-sm font-semibold text-slate-900">
                  {formatAmount(account.balance.amount)}
                </p>
              </div>
            </div>
          </button>
        </li>
      ))}
    </ul>
  )
}

interface OverviewStatCardProps {
  title: string
  items: Array<{ label: string; value: string }>
}

export function OverviewStatCard({ title, items }: OverviewStatCardProps) {
  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <h3 className="text-sm font-semibold text-slate-900">{title}</h3>

      <ul className="mt-6 space-y-3">
        {items.map((item) => (
          <li
            key={item.label}
            className="flex items-center justify-between gap-3 text-sm"
          >
            <span className="text-slate-600">{item.label}</span>
            <span className="font-semibold text-slate-900">{item.value}</span>
          </li>
        ))}
      </ul>
    </article>
  )
}

interface NestedTabButtonProps {
  label: string
  isActive: boolean
  onClick: () => void
}

export function NestedTabButton({
  label,
  isActive,
  onClick,
}: NestedTabButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-xl px-4 py-2 text-sm font-medium transition ${
        isActive
          ? 'bg-white text-slate-900 shadow-sm'
          : 'text-slate-500 hover:text-slate-800'
      }`}
    >
      {label}
    </button>
  )
}

interface CurrencyTotalPillsProps {
  totals: CurrencyTotalsDto
}

export function CurrencyTotalPills({ totals }: CurrencyTotalPillsProps) {
  const entries = Object.entries(totals).sort(([left], [right]) =>
    left.localeCompare(right),
  )

  if (entries.length === 0) {
    return <span className="text-xs text-slate-500">Нет данных</span>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {entries.map(([currency, amount]) => (
        <span
          key={currency}
          className="rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700"
        >
          {currency} {formatAmount(amount)}
        </span>
      ))}
    </div>
  )
}
