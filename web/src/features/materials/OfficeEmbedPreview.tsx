import { useEffect, useState } from 'react'
import { api, ApiError } from '../../shared/api/client'
import { Icon } from '../../shared/components/Icon'
import { Spinner } from '../../shared/components/Spinner'
import { ErrorBanner } from '../../shared/components/ErrorBanner'

interface ViewUrlResponse {
  url: string
  expiresInSeconds: number
}

interface OfficeEmbedPreviewProps {
  materialId: number
  versionId: number
  fileName: string
  onDownload: () => void
}

export function OfficeEmbedPreview({ materialId, versionId, fileName, onDownload }: OfficeEmbedPreviewProps) {
  const [embedUrl, setEmbedUrl] = useState<string | null>(null)
  const [unavailable, setUnavailable] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      setUnavailable(false)
      try {
        const res = await api.get<ViewUrlResponse>(`/materials/${materialId}/versions/${versionId}/view-url`)
        if (cancelled) return
        setEmbedUrl(`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(res.url)}`)
      } catch (err) {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 409) {
          setUnavailable(true)
        } else {
          setError(err)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [materialId, versionId])

  if (loading) return <Spinner />

  if (unavailable) {
    return (
      <div className="stack" style={{ alignItems: 'center', padding: 'var(--space-6) 0', textAlign: 'center' }}>
        <Icon name="file" size={40} />
        <p style={{ marginBottom: 0 }}>
          This file can&apos;t be previewed here because the server isn&apos;t using cloud storage. Download it to
          view it.
        </p>
        <button className="btn btn-primary" onClick={onDownload}>
          <Icon name="download" size={16} />
          Download
        </button>
      </div>
    )
  }

  if (error) return <ErrorBanner error={error} />

  if (!embedUrl) return null

  return (
    <iframe
      src={embedUrl}
      title={fileName}
      className="office-embed-frame"
      style={{ border: 'none', width: '100%', maxWidth: '100%' }}
    />
  )
}
