import { useRef, useState } from 'react'

interface ImagePreviewProps {
  blobUrl: string
  fileName: string
}

const ZOOMED_SCALE = 2.5

export function ImagePreview({ blobUrl, fileName }: ImagePreviewProps) {
  const [zoomed, setZoomed] = useState(false)
  const [origin, setOrigin] = useState('50% 50%')
  const imgRef = useRef<HTMLImageElement | null>(null)

  function toggleZoom(e: React.MouseEvent<HTMLImageElement>) {
    if (!zoomed && imgRef.current) {
      const rect = imgRef.current.getBoundingClientRect()
      const x = ((e.clientX - rect.left) / rect.width) * 100
      const y = ((e.clientY - rect.top) / rect.height) * 100
      setOrigin(`${x}% ${y}%`)
    }
    setZoomed((z) => !z)
  }

  return (
    <div className="image-preview-wrap">
      <img
        ref={imgRef}
        src={blobUrl}
        alt={fileName}
        className="viewer-image image-preview-img"
        style={{
          transform: zoomed ? `scale(${ZOOMED_SCALE})` : 'scale(1)',
          transformOrigin: origin,
          cursor: zoomed ? 'zoom-out' : 'zoom-in',
        }}
        onDoubleClick={toggleZoom}
        onClick={(e) => {
          if (zoomed) toggleZoom(e)
        }}
      />
    </div>
  )
}
