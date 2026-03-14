import type { ApiErrorDto } from './types'

type QueryValue = string | number | boolean | null | undefined
export type QueryParams = Record<string, QueryValue>

export interface RequestJsonOptions {
  query?: QueryParams
  signal?: AbortSignal
}

export interface RequestFormDataOptions {
  query?: QueryParams
  signal?: AbortSignal
}

export interface HttpClient {
  getJson<T>(path: string, options?: RequestJsonOptions): Promise<T>
  postJson<TResponse, TBody>(path: string, body: TBody, options?: RequestJsonOptions): Promise<TResponse>
  putJson<TResponse, TBody>(path: string, body: TBody, options?: RequestJsonOptions): Promise<TResponse>
  putVoid(path: string, options?: RequestJsonOptions): Promise<void>
  deleteJson<T>(path: string, options?: RequestJsonOptions): Promise<T>
  deleteVoid(path: string, options?: RequestJsonOptions): Promise<void>
  postFormData<TResponse>(
    path: string,
    formData: FormData,
    options?: RequestFormDataOptions,
  ): Promise<TResponse>
}

export interface HttpClientConfig {
  baseUrl?: string
  fetchFn?: typeof fetch
}

export class ApiClientError extends Error {
  readonly status: number
  readonly code: string | null

  constructor(message: string, status: number, code: string | null) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = code
  }
}

const DEFAULT_API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export function createHttpClient(config: HttpClientConfig = {}): HttpClient {
  const baseUrl = config.baseUrl ?? DEFAULT_API_BASE_URL
  const fetchFn = config.fetchFn ?? fetch

  return {
    async getJson<T>(path: string, options: RequestJsonOptions = {}): Promise<T> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
        },
        signal: options.signal,
      })

      return parseJsonResponse<T>(response)
    },

    async postJson<TResponse, TBody>(
      path: string,
      body: TBody,
      options: RequestJsonOptions = {},
    ): Promise<TResponse> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
        signal: options.signal,
      })

      return parseJsonResponse<TResponse>(response)
    },

    async putJson<TResponse, TBody>(
      path: string,
      body: TBody,
      options: RequestJsonOptions = {},
    ): Promise<TResponse> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'PUT',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
        signal: options.signal,
      })

      return parseJsonResponse<TResponse>(response, { allowEmpty: true })
    },

    async putVoid(path: string, options: RequestJsonOptions = {}): Promise<void> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'PUT',
        headers: {
          Accept: 'application/json',
        },
        signal: options.signal,
      })

      return parseJsonResponse<void>(response, { allowEmpty: true })
    },

    async deleteJson<T>(path: string, options: RequestJsonOptions = {}): Promise<T> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'DELETE',
        headers: {
          Accept: 'application/json',
        },
        signal: options.signal,
      })

      return parseJsonResponse<T>(response, { allowEmpty: true })
    },

    async deleteVoid(path: string, options: RequestJsonOptions = {}): Promise<void> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'DELETE',
        headers: {
          Accept: 'application/json',
        },
        signal: options.signal,
      })

      return parseJsonResponse<void>(response, { allowEmpty: true })
    },

    async postFormData<TResponse>(
      path: string,
      formData: FormData,
      options: RequestFormDataOptions = {},
    ): Promise<TResponse> {
      const url = buildUrl(baseUrl, path, options.query)
      const response = await fetchFn(url, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
        },
        body: formData,
        signal: options.signal,
      })

      return parseJsonResponse<TResponse>(response)
    },
  }
}

function buildUrl(baseUrl: string, path: string, query?: QueryParams): string {
  const normalizedBase = trimTrailingSlash(baseUrl)
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin)

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value))
      }
    }
  }

  return url.toString()
}

async function parseJsonResponse<T>(
  response: Response,
  options: { allowEmpty?: boolean } = {},
): Promise<T> {
  const body = await readResponseBody(response)

  if (!response.ok) {
    const apiError = isApiErrorDto(body) ? body : null
    const message = apiError?.message ?? `Request failed with status ${response.status}`
    const code = apiError?.error ?? null
    throw new ApiClientError(message, response.status, code)
  }

  if (body === null) {
    if (options.allowEmpty) {
      return undefined as T
    }
    throw new ApiClientError('Expected JSON response body but got empty response', response.status, null)
  }

  return body as T
}

async function readResponseBody(response: Response): Promise<unknown | null> {
  const rawText = await response.text()
  if (rawText.length === 0) {
    return null
  }

  try {
    return JSON.parse(rawText) as unknown
  } catch {
    return rawText
  }
}

function isApiErrorDto(value: unknown): value is ApiErrorDto {
  if (!value || typeof value !== 'object') {
    return false
  }

  const candidate = value as Record<string, unknown>
  return typeof candidate.error === 'string' && typeof candidate.message === 'string'
}

function trimTrailingSlash(value: string): string {
  if (value.endsWith('/')) {
    return value.slice(0, -1)
  }
  return value
}
