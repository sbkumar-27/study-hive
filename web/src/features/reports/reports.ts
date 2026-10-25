import { api } from '../../shared/api/client'
import type { PageResponse, Report, ReportStatus } from '../../shared/types'

export interface CreateReportRequest {
  materialId: number
  reason: string
}

export const reportsApi = {
  create: (req: CreateReportRequest) => api.post<Report>('/reports', req),
  listPending: (page = 0, size = 20) => api.get<PageResponse<Report>>(`/reports?page=${page}&size=${size}`),
  resolve: (id: number, status: ReportStatus) => api.put<Report>(`/reports/${id}`, { status }),
}
