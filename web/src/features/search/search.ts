import { api } from '../../shared/api/client'
import type { PageResponse, SearchResult } from '../../shared/types'

function qs(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const str = search.toString()
  return str ? `?${str}` : ''
}

export const searchApi = {
  search: (q: string, page = 0, size = 20) =>
    api.get<PageResponse<SearchResult>>(`/search${qs({ q, page, size })}`),
}
