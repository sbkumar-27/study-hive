import { api } from '../../shared/api/client'
import type { Subject } from '../../shared/types'

export interface CreateSubjectRequest {
  subjectCode: string
  subjectName: string
}

export const subjectsApi = {
  list: (communityId: number) => api.get<Subject[]>(`/communities/${communityId}/subjects`),
  create: (communityId: number, req: CreateSubjectRequest) =>
    api.post<Subject>(`/communities/${communityId}/subjects`, req),
}
