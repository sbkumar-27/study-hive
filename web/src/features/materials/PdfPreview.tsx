import { useLayoutEffect, useRef, useState } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'
import pdfWorkerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { Spinner } from '../../shared/components/Spinner'
import { ErrorBanner } from '../../shared/components/ErrorBanner'

pdfjs.GlobalWorkerOptions.workerSrc = pdfWorkerSrc

const MIN_SCALE = 0.75
const MAX_SCALE = 2.5
const SCALE_STEP = 0.25

interface PdfPreviewProps {
  blob: Blob
  fileName: string
}

export function PdfPreview({ blob, fileName }: PdfPreviewProps) {
  const [numPages, setNumPages] = useState<number | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [scale, setScale] = useState(1)
  const [containerWidth, setContainerWidth] = useState(0)
  const [loadError, setLoadError] = useState<unknown>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)

  useLayoutEffect(() => {
    const el = containerRef.current
    if (!el) return
    const update = () => setContainerWidth(el.getBoundingClientRect().width)
    update()
    const observer = new ResizeObserver(update)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const pageWidth = containerWidth > 0 ? Math.max(containerWidth - 16, 50) * scale : undefined

  return (
    <div className="pdf-preview">
      <div className="pdf-preview-toolbar row-between">
        <div className="row" style={{ gap: 8 }}>
          <button
            type="button"
            className="btn btn-sm btn-icon"
            onClick={() => setScale((s) => Math.max(MIN_SCALE, +(s - SCALE_STEP).toFixed(2)))}
            disabled={scale <= MIN_SCALE}
            aria-label="Zoom out"
          >
            −
          </button>
          <span className="text-sm muted">{Math.round(scale * 100)}%</span>
          <button
            type="button"
            className="btn btn-sm btn-icon"
            onClick={() => setScale((s) => Math.min(MAX_SCALE, +(s + SCALE_STEP).toFixed(2)))}
            disabled={scale >= MAX_SCALE}
            aria-label="Zoom in"
          >
            +
          </button>
        </div>
        {numPages !== null && (
          <span className="text-sm muted">
            Page {currentPage} of {numPages}
          </span>
        )}
      </div>

      <div className="pdf-preview-scroll" ref={containerRef}>
        {loadError ? (
          <ErrorBanner error={loadError} />
        ) : (
          <Document
            file={blob}
            onLoadSuccess={({ numPages: n }) => setNumPages(n)}
            onLoadError={(err) => setLoadError(err)}
            loading={<Spinner />}
          >
            {Array.from({ length: numPages ?? 0 }, (_, i) => i + 1).map((pageNumber) => (
              <div
                key={pageNumber}
                className="pdf-preview-page"
                onClick={() => setCurrentPage(pageNumber)}
              >
                <Page
                  pageNumber={pageNumber}
                  width={pageWidth}
                  renderAnnotationLayer
                  renderTextLayer
                  loading={<Spinner />}
                />
              </div>
            ))}
          </Document>
        )}
      </div>

      <div className="pdf-preview-nav row" style={{ justifyContent: 'center', gap: 10 }}>
        <button
          type="button"
          className="btn btn-sm"
          onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
          disabled={currentPage <= 1}
        >
          Prev
        </button>
        <button
          type="button"
          className="btn btn-sm"
          onClick={() => setCurrentPage((p) => Math.min(numPages ?? p, p + 1))}
          disabled={numPages === null || currentPage >= numPages}
        >
          Next
        </button>
      </div>
      <p className="muted" style={{ textAlign: 'center', fontSize: '0.7rem', margin: 0 }}>
        {fileName}
      </p>
    </div>
  )
}
