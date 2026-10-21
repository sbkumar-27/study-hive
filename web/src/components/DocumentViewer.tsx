import { useEffect, useRef, useState } from 'react'
import { authHeader, downloadUrl } from '../api/client'
import { Icon } from './Icon'
import { Spinner } from './Spinner'
import { ErrorBanner } from './ErrorBanner'

type Kind = 'pdf' | 'image' | 'text' | 'unsupported'

function classify(mimeType: string | null, fileName: string): Kind {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? ''
  if (mimeType === 'application/pdf' || ext === 'pdf') return 'pdf'
  if ((mimeType?.startsWith('image/') ?? false) || ['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'image'
  if ((mimeType?.startsWith('text/') ?? false) || ['txt', 'md'].includes(ext)) return 'text'
  return 'unsupported'
}

interface DocumentViewerProps {
  materialId: number
  versionId: number
  fileName: string
  mimeType: string | null
  onClose: () => void
  onDownload: () => void
}

export function DocumentViewer({ materialId, versionId, fileName, mimeType, onClose, onDownload }: DocumentViewerProps) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null)
  const [textContent, setTextContent] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const blobUrlRef = useRef<string | null>(null)
  const kind = classify(mimeType, fileName)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const response = await fetch(
          downloadUrl(`/materials/${materialId}/versions/${versionId}/preview`),
          { headers: authHeader() },
        )
        if (!response.ok) {
          throw new Error(`Preview failed with status ${response.status}`)
        }
        const blob = await response.blob()
        if (cancelled) return

        if (kind === 'text') {
          setTextContent(await blob.text())
        } else if (kind === 'pdf' || kind === 'image') {
          const url = URL.createObjectURL(blob)
          blobUrlRef.current = url
          setBlobUrl(url)
        }
        // 'unsupported' kind never fetches a blob URL -- just shows the fallback below.
      } catch (err) {
        if (!cancelled) setError(err)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    if (kind !== 'unsupported') {
      load()
    } else {
      setLoading(false)
    }

    return () => {
      cancelled = true
    }
  }, [materialId, versionId, kind])

  useEffect(
    () => () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current)
      }
    },
    [],
  )

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div
      className="viewer-backdrop"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="viewer-panel card" role="dialog" aria-modal="true" aria-label={`Preview of ${fileName}`}>
        <div className="row-between viewer-header">
          <strong className="text-sm" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {fileName}
          </strong>
          <div className="row" style={{ gap: 8 }}>
            <button className="btn btn-sm" onClick={onDownload}>
              <Icon name="download" size={13} />
              Download
            </button>
            <button className="btn btn-sm btn-icon" onClick={onClose} aria-label="Close preview">
              <Icon name="close" size={15} />
            </button>
          </div>
        </div>

        <div className="viewer-body">
          {loading && <Spinner />}
          {!loading && <ErrorBanner error={error} />}

          {!loading && !error && kind === 'pdf' && blobUrl && (
            <iframe src={blobUrl} title={fileName} className="viewer-frame" />
          )}

          {!loading && !error && kind === 'image' && blobUrl && (
            <img src={blobUrl} alt={fileName} className="viewer-image" />
          )}

          {!loading && !error && kind === 'text' && textContent !== null && (
            <pre className="viewer-text">{textContent}</pre>
          )}

          {!loading && !error && kind === 'unsupported' && (
            <div className="stack" style={{ alignItems: 'center', padding: 'var(--space-6) 0' }}>
              <Icon name="file" size={40} />
              <p style={{ marginBottom: 0 }}>Preview not available for this file type.</p>
              <button className="btn btn-primary" onClick={onDownload}>
                <Icon name="download" size={16} />
                Download
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
