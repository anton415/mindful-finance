import { useEffect, useId, useMemo, useRef, useState } from 'react'
import {
  ApiClientError,
  apiClient,
  type InstrumentOptionDto,
} from '../../api'
import {
  isMoexTradeAccountType,
  toAccountTypeLabel,
  toInstrumentKindLabel,
} from './formatting'
import { normalizeInstrumentSymbol } from './transactionTrade'
import type { AccountWithBalance } from './types'

type SearchStatus = 'idle' | 'loading' | 'ready' | 'error'

interface InstrumentSearchFieldProps {
  account: AccountWithBalance | null
  value: string
  onChange: (symbol: string) => void
}

export function InstrumentSearchField({
  account,
  value,
  onChange,
}: InstrumentSearchFieldProps) {
  const normalizedValue = normalizeInstrumentSymbol(value)
  const [query, setQuery] = useState<string>(normalizedValue)
  const [options, setOptions] = useState<InstrumentOptionDto[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const listboxId = useId()
  const blurTimeoutRef = useRef<number | null>(null)
  const skipNextValueSyncRef = useRef<boolean>(false)

  const supportsMoexSearch =
    account !== null && isMoexTradeAccountType(account.type)
  const exactMatchOption = useMemo(
    () => options.find((option) => option.symbol === normalizedValue) ?? null,
    [options, normalizedValue],
  )
  const showUnresolvedValue =
    supportsMoexSearch &&
    normalizedValue.length > 0 &&
    query === normalizedValue &&
    status === 'ready' &&
    exactMatchOption === null

  useEffect(() => {
    if (skipNextValueSyncRef.current) {
      skipNextValueSyncRef.current = false
      return
    }

    const timeoutId = window.setTimeout(() => {
      setQuery(normalizedValue)
      setOptions([])
      setStatus('idle')
      setErrorMessage(null)
      setIsOpen(false)
    }, 0)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [normalizedValue])

  useEffect(() => {
    if (blurTimeoutRef.current !== null) {
      window.clearTimeout(blurTimeoutRef.current)
      blurTimeoutRef.current = null
    }

    if (!supportsMoexSearch) {
      return
    }

    const trimmedQuery = query.trim()
    if (trimmedQuery.length < 2) {
      return
    }

    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
      setStatus('loading')
      setIsOpen(true)

      void apiClient
        .searchAccountInstruments(account.id, trimmedQuery, controller.signal)
        .then((rows) => {
          if (controller.signal.aborted) {
            return
          }

          setOptions(rows)
          setStatus('ready')
          setErrorMessage(null)
        })
        .catch((error: unknown) => {
          if (
            controller.signal.aborted ||
            (error instanceof DOMException && error.name === 'AbortError')
          ) {
            return
          }

          setOptions([])
          setStatus('error')
          setErrorMessage(toInstrumentSearchErrorMessage(error))
        })
    }, 250)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [account?.id, query, supportsMoexSearch])

  const handleInputChange = (nextValue: string): void => {
    const normalizedQuery = normalizeInstrumentSymbol(nextValue)
    setQuery(normalizedQuery)
    setOptions([])
    setStatus('idle')
    setErrorMessage(null)
    setIsOpen(normalizedQuery.trim().length >= 2)

    if (normalizedQuery !== normalizedValue) {
      skipNextValueSyncRef.current = true
      onChange('')
    }
  }

  const handleSelectOption = (option: InstrumentOptionDto): void => {
    setQuery(option.symbol)
    setIsOpen(false)
    onChange(option.symbol)
  }

  const handleInputBlur = (): void => {
    blurTimeoutRef.current = window.setTimeout(() => {
      setIsOpen(false)
      blurTimeoutRef.current = null
    }, 120)
  }

  const handleInputFocus = (): void => {
    if (blurTimeoutRef.current !== null) {
      window.clearTimeout(blurTimeoutRef.current)
      blurTimeoutRef.current = null
    }

    if (supportsMoexSearch && query.trim().length >= 2) {
      setIsOpen(true)
    }
  }

  if (account === null) {
    return (
      <div className="mt-1 space-y-2">
        <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
          Сначала выберите счет
        </div>
      </div>
    )
  }

  if (!supportsMoexSearch) {
    return (
      <div className="mt-1 space-y-2">
        <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
          {normalizedValue.length > 0 ? normalizedValue : 'Выбор инструмента недоступен'}
        </div>
        <p className="text-[11px] text-slate-500">
          Для счета типа {toAccountTypeLabel(account.type)} подбор инструментов с
          Московской биржи не поддерживается.
        </p>
      </div>
    )
  }

  return (
    <div className="mt-1">
      <div className="relative">
        <input
          type="text"
          value={query}
          onChange={(event) => handleInputChange(event.target.value)}
          onBlur={handleInputBlur}
          onFocus={handleInputFocus}
          placeholder="Начните вводить символ"
          autoComplete="off"
          aria-expanded={isOpen}
          aria-controls={isOpen ? listboxId : undefined}
          className="block w-full rounded-md border border-slate-300 bg-white px-2 py-2 text-sm text-slate-800"
        />

        {isOpen ? (
          <div
            className="absolute z-20 mt-1 w-full rounded-md border border-slate-200 bg-white shadow-lg"
            role="listbox"
            id={listboxId}
          >
            {status === 'loading' ? (
              <p className="px-3 py-2 text-xs text-slate-500">
                Ищем инструменты на MOEX...
              </p>
            ) : null}

            {status === 'error' && errorMessage ? (
              <p className="px-3 py-2 text-xs text-amber-700">{errorMessage}</p>
            ) : null}

            {status === 'ready' && options.length === 0 ? (
              <p className="px-3 py-2 text-xs text-slate-500">
                Ничего не найдено на Московской бирже.
              </p>
            ) : null}

            {options.length > 0 ? (
              <ul className="max-h-64 overflow-y-auto py-1">
                {options.map((option) => {
                  const isSelected = option.symbol === normalizedValue
                  return (
                    <li key={option.symbol}>
                      <button
                        type="button"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => handleSelectOption(option)}
                        className={`flex w-full items-start justify-between gap-3 px-3 py-2 text-left ${
                          isSelected
                            ? 'bg-slate-100 text-slate-900'
                            : 'text-slate-700 hover:bg-slate-50'
                        }`}
                      >
                        <span className="min-w-0">
                          <span className="block text-sm font-medium">
                            {option.symbol}
                          </span>
                          <span className="block truncate text-xs text-slate-500">
                            {toOptionSecondaryText(option)}
                          </span>
                        </span>
                        <span className="rounded-full border border-slate-200 px-2 py-0.5 text-[10px] uppercase tracking-wide text-slate-500">
                          {toInstrumentKindLabel(option.kind)}
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            ) : null}
          </div>
        ) : null}
      </div>

      <div className="mt-2 space-y-1">
        {query.trim().length < 2 ? (
          <p className="text-[11px] text-slate-500">
            Введите минимум 2 символа для поиска по MOEX.
          </p>
        ) : null}

        {showUnresolvedValue ? (
          <p className="text-[11px] text-slate-500">
            Сохранен символ {normalizedValue}, но сейчас его нет в выдаче MOEX
            для этого типа счета. Вы можете оставить его как есть или выбрать
            новый инструмент из списка.
          </p>
        ) : null}
      </div>
    </div>
  )
}

function toOptionSecondaryText(option: InstrumentOptionDto): string {
  const parts = [option.shortName ?? option.name, option.isin]
    .filter((value): value is string => value !== null && value.length > 0)
    .slice(0, 2)

  return parts.join(' · ')
}

function toInstrumentSearchErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Не удалось загрузить инструменты.'
}
