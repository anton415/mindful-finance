import {
  formatAmount,
  toAccountStatusLabel,
  toAccountTypeLabel,
  toInvestmentAccountCountLabel,
} from './formatting'
import { buildInvestmentOverview } from './model'
import type { AccountWithBalance } from './types'
import {
  CurrencyTotalPills,
  MetricCard,
  OverviewStatCard,
  StatusCard,
} from './ui'

interface InvestmentOverviewTabProps {
  accounts: AccountWithBalance[]
  selectedAccountId: string | null
  onOpenTransactions: (accountId: string) => void
  onOpenSettings: (accountId: string) => void
  onOpenSettingsWithoutSelection: () => void
}

export function InvestmentOverviewTab({
  accounts,
  selectedAccountId,
  onOpenTransactions,
  onOpenSettings,
  onOpenSettingsWithoutSelection,
}: InvestmentOverviewTabProps) {
  if (accounts.length === 0) {
    return (
      <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <StatusCard
          tone="neutral"
          message="Пока нет инвестиционных счетов. Откройте настройки и добавьте первый счет."
        />
        <button
          type="button"
          onClick={onOpenSettingsWithoutSelection}
          className="mt-4 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800"
        >
          Открыть настройки
        </button>
      </article>
    )
  }

  const overview = buildInvestmentOverview(accounts)
  const selectedAccount =
    accounts.find((account) => account.id === selectedAccountId) ?? null

  return (
    <div className="space-y-4">
      <div className="grid gap-4 xl:grid-cols-3">
        <MetricCard
          title="Баланс по валютам"
          subtitle="Текущие суммы без автоматической FX-конвертации."
          totals={overview.totalsByCurrency}
        />
        <OverviewStatCard
          title="Базовые метрики"
          items={[
            {
              label: 'Всего счетов',
              value: String(overview.totalAccountCount),
            },
            {
              label: 'Активные',
              value: String(overview.activeAccountCount),
            },
            {
              label: 'Архивные',
              value: String(overview.archivedAccountCount),
            },
          ]}
        />
        <OverviewStatCard
          title="Покрытие обзора"
          items={[
            {
              label: 'Валют в портфеле',
              value: String(overview.currencyCount),
            },
            {
              label: 'Типов счетов',
              value: String(overview.typeBreakdown.length),
            },
            {
              label: 'Выбранный счет',
              value: selectedAccount?.name ?? 'Не выбран',
            },
          ]}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
        <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <h3 className="text-sm font-semibold text-slate-900">
            Структура по типам счетов
          </h3>
          <p className="mt-1 text-xs leading-relaxed text-slate-500">
            Помогает быстро увидеть, где лежит капитал по типам инвестиционных
            контуров.
          </p>

          <ul className="mt-4 space-y-3">
            {overview.typeBreakdown.map((summary) => (
              <li
                key={summary.type}
                className="rounded-lg border border-slate-200 bg-white px-3 py-3"
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">
                      {toAccountTypeLabel(summary.type)}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {summary.accountCount}{' '}
                      {toInvestmentAccountCountLabel(summary.accountCount)}
                    </p>
                  </div>
                  <CurrencyTotalPills totals={summary.totalsByCurrency} />
                </div>
              </li>
            ))}
          </ul>
        </article>

        <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <h3 className="text-sm font-semibold text-slate-900">
            Счета портфеля
          </h3>
          <p className="mt-1 text-xs leading-relaxed text-slate-500">
            Из обзора можно сразу перейти к транзакциям или настройкам нужного
            счета.
          </p>

          <ul className="mt-4 space-y-2">
            {accounts.map((account) => (
              <li
                key={account.id}
                className={`rounded-lg border px-3 py-3 ${
                  selectedAccountId === account.id
                    ? 'border-slate-300 bg-white shadow-sm'
                    : 'border-slate-200 bg-white'
                }`}
              >
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">
                      {account.name}
                    </p>
                    <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
                      {toAccountTypeLabel(account.type)} ·{' '}
                      {toAccountStatusLabel(account.status)}
                    </p>
                  </div>

                  <div className="flex flex-col items-start gap-3 lg:items-end">
                    <div className="text-left lg:text-right">
                      <p className="text-xs uppercase tracking-wide text-slate-500">
                        {account.balance.currency}
                      </p>
                      <p className="mt-1 text-sm font-semibold text-slate-900">
                        {formatAmount(account.balance.amount)}
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => onOpenTransactions(account.id)}
                        className="rounded-md border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-800"
                      >
                        Транзакции
                      </button>
                      <button
                        type="button"
                        onClick={() => onOpenSettings(account.id)}
                        className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700"
                      >
                        Настройки
                      </button>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </article>
      </div>
    </div>
  )
}
