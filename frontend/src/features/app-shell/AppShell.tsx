import { type ReactNode } from 'react'

export type ViewTab = 'dashboard' | 'accounts' | 'personal-finance'

interface AppShellProps {
  activeView: ViewTab
  headerContextLabel: string
  onSelectView: (view: ViewTab) => void
  children: ReactNode
}

interface ViewCopy {
  label: string
  description: string
}

interface TabButtonProps {
  label: string
  isActive: boolean
  onClick: () => void
}

const VIEW_COPY: Record<ViewTab, ViewCopy> = {
  dashboard: {
    label: 'Обзор',
    description: 'Спокойный срез капитала и метрик финансового спокойствия.',
  },
  accounts: {
    label: 'Инвестиции',
    description:
      'Инвестиционные счета и активы с балансами, деталями транзакций и простыми фильтрами.',
  },
  'personal-finance': {
    label: 'Личные финансы',
    description:
      'Ручной yearly review по картам: факт расходов, лимиты, фактический доход и прогноз.',
  },
}

const VIEW_ORDER: ViewTab[] = ['dashboard', 'accounts', 'personal-finance']

export function AppShell({
  activeView,
  headerContextLabel,
  onSelectView,
  children,
}: AppShellProps) {
  const activeViewCopy = VIEW_COPY[activeView]

  return (
    <main className="min-h-screen w-full px-4 py-8 sm:px-6 sm:py-14">
      <div className="space-y-6">
        <nav
          aria-label="Основные разделы"
          className="rounded-2xl border border-slate-200 bg-white/85 p-2 shadow-sm backdrop-blur"
        >
          <div className="flex flex-wrap gap-2">
            {VIEW_ORDER.map((view) => (
              <TabButton
                key={view}
                label={VIEW_COPY[view].label}
                isActive={activeView === view}
                onClick={() => onSelectView(view)}
              />
            ))}
          </div>
        </nav>

        <section className="rounded-2xl border border-slate-200 bg-white/85 p-5 shadow-sm backdrop-blur lg:p-8">
          <header className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-medium uppercase tracking-[0.2em] text-slate-500">
                Mindful Finance
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-900">
                {activeViewCopy.label}
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-relaxed text-slate-600">
                {activeViewCopy.description}
              </p>
            </div>
            <p className="text-xs uppercase tracking-wide text-slate-500">
              {headerContextLabel}
            </p>
          </header>

          <section className="mt-6">{children}</section>
        </section>
      </div>
    </main>
  )
}

function TabButton({ label, isActive, onClick }: TabButtonProps) {
  const activeClasses = isActive
    ? 'bg-white text-slate-900 shadow-sm'
    : 'bg-transparent text-slate-500 hover:text-slate-800'

  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${activeClasses}`}
    >
      {label}
    </button>
  )
}
