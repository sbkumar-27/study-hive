import { api, authHeader, downloadUrl } from './client'
import type { Material, MaterialType, MaterialVersion, PageResponse, SortOrder } from '../types'

export interface CreateMaterialRequest {
  title: string
  description?: string
  materialType?: MaterialType
  communityId?: number
  subjectId?: number
  bookName?: string
  authorName?: string
  examYear?: number
  examType?: string
  semester?: number
}

export interface UpdateMaterialRequest {
  title: string
  description?: string
  bookName?: string
  authorName?: string
}

export interface MaterialSearchParams {
  q?: string
  communityId?: number
  subjectId?: number
  type?: MaterialType
  sort?: SortOrder
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

export const materialsApi = {
  upload: (file: File, metadata: CreateMaterialRequest) => {
    const form = new FormData()
    form.set('file', file)
    form.set('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
    return api.postForm<Material>('/materials', form)
  },

  search: (params: MaterialSearchParams) =>
    api.get<PageResponse<Material>>(`/materials${qs({ ...params })}`),

  vault: (page = 0, size = 20) => api.get<PageResponse<Material>>(`/materials/vault${qs({ page, size })}`),

  get: (id: number) => api.get<Material>(`/materials/${id}`),

  update: (id: number, req: UpdateMaterialRequest) => api.put<Material>(`/materials/${id}`, req),

  favorite: (id: number) => api.post<void>(`/materials/${id}/favorite`),
  unfavorite: (id: number) => api.del<void>(`/materials/${id}/favorite`),

  recordView: (id: number) => api.post<{ viewCount: number }>(`/materials/${id}/view`),

  versions: (id: number) => api.get<MaterialVersion[]>(`/materials/${id}/versions`),

  addVersion: (id: number, file: File, versionNotes?: string) => {
    const form = new FormData()
    form.set('file', file)
    if (versionNotes) form.set('versionNotes', versionNotes)
    return api.postForm<MaterialVersion>(`/materials/${id}/versions`, form)
  },

  async downloadLatest(id: number, filenameHint: string) {
    await downloadBlob(downloadUrl(`/materials/${id}/download`), filenameHint)
  },

  async downloadVersion(materialId: number, versionId: number, filenameHint: string) {
    await downloadBlob(downloadUrl(`/materials/${materialId}/versions/${versionId}/download`), filenameHint)
  },
}

async function downloadBlob(url: string, filenameHint: string) {
  const response = await fetch(url, { headers: authHeader() })
  if (!response.ok) {
    throw new Error(`Download failed with status ${response.status}`)
  }
  const disposition = response.headers.get('content-disposition')
  const match = disposition?.match(/filename="?([^"]+)"?/)
  const filename = match?.[1] ?? filenameHint

  const blob = await response.blob()
  const blobUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(blobUrl)
}
