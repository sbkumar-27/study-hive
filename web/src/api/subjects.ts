import { api } from './client'
import type { Subject } from '../types'

export interface CreateSubjectRequest {
  subjectCode: string
  subjectName: string
}

export const subjectsApi = {
  list: (communityId: number) => api.get<Subject[]>(`/communities/${communityId}/subjects`),
  create: (communityId: number, req: CreateSubjectRequest) =>
    api.post<Subject>(`/communities/${communityId}/subjects`, req),
}
