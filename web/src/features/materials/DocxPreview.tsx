import { useEffect, useRef, useState } from 'react'
import { renderAsync } from 'docx-preview'
import { Spinner } from '../../shared/components/Spinner'
import { ErrorBanner } from '../../shared/components/ErrorBanner'

interface DocxPreviewProps {
  blob: Blob
}

export function DocxPreview({ blob }: DocxPreviewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    let cancelled = false
    const container = containerRef.current
    if (!container) return

    setLoading(true)
    setError(null)
    container.innerHTML = ''

    renderAsync(blob, container, undefined, {
      inWrapper: true,
      ignoreWidth: false,
      ignoreHeight: true,
      className: 'docx-preview-doc',
    })
      .catch((err) => {
        if (!cancelled) setError(err)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [blob])

  return (
    <div className="docx-preview-scroll">
      {loading && <Spinner />}
      {!loading && <ErrorBanner error={error} />}
      <div
        ref={containerRef}
        className="docx-preview-container"
        style={{ display: loading ? 'none' : 'block' }}
      />
    </div>
  )
}
