import type { ApiErrorBody } from '../types'

const API_BASE = '/api/v1'

export class ApiError extends Error {
  status: number
  fieldErrors: string[] | null

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.status = body.status
    this.fieldErrors = body.fieldErrors
  }
}

let authToken: string | null = null

export function setAuthToken(token: string | null) {
  authToken = token
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  body?: unknown
  isFormData?: boolean
  signal?: AbortSignal
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`
  }

  let body: BodyInit | undefined
  if (options.body !== undefined) {
    if (options.isFormData) {
      body = options.body as FormData
    } else {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(options.body)
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body,
    signal: options.signal,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json')

  if (!response.ok) {
    if (isJson) {
      const errorBody = (await response.json()) as ApiErrorBody
      throw new ApiError(errorBody)
    }
    throw new Error(`Request failed with status ${response.status}`)
  }

  if (!isJson) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>(path, { method: 'GET', signal }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForm: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: 'POST', body: formData, isFormData: true }),
}

export function downloadUrl(path: string): string {
  return `${API_BASE}${path}`
}

export function authHeader(): Record<string, string> {
  return authToken ? { Authorization: `Bearer ${authToken}` } : {}
}
