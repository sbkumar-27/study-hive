import { api } from './client'
import type { PageResponse, QuestionPaper } from '../types'

export interface QuestionPaperSearchParams {
  subjectId?: number
  examYear?: number
  examType?: string
  semester?: number
  page?: number
  size?: number
}

function qs(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const str = search.toString()
  return str ? `?${str}` : ''
}

export const questionPapersApi = {
  search: (params: QuestionPaperSearchParams) =>
    api.get<PageResponse<QuestionPaper>>(`/question-papers${qs({ ...params })}`),
}
