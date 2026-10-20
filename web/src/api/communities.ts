import { api } from './client'
import type { Community, CommunityMember, CommunityType, PageResponse } from '../types'

export interface CreateCommunityRequest {
  name: string
  description?: string
  communityType?: CommunityType
  privatePassword?: string
}

export interface JoinCommunityRequest {
  joinCode: string
  password?: string
}

function qs(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const str = search.toString()
  return str ? `?${str}` : ''
}

export const communitiesApi = {
  create: (req: CreateCommunityRequest) => api.post<Community>('/communities', req),
  search: (q: string | undefined, page: number, size = 20) =>
    api.get<PageResponse<Community>>(`/communities${qs({ q, page, size })}`),
  mine: (page: number, size = 20) => api.get<PageResponse<Community>>(`/communities/mine${qs({ page, size })}`),
  get: (id: number) => api.get<Community>(`/communities/${id}`),
  join: (req: JoinCommunityRequest) => api.post<Community>('/communities/join', req),
  leave: (id: number) => api.post<void>(`/communities/${id}/leave`),
  members: (id: number) => api.get<CommunityMember[]>(`/communities/${id}/members`),
}
