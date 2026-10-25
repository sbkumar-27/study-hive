/** Human-readable file size, e.g. 820 B / 14.3 KB / 2.1 MB. */
export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** The file extension from a name, uppercased (e.g. "notes.pdf" -> "PDF"); "" if there isn't one. */
export function fileExtensionOf(fileName: string | null | undefined): string {
  if (!fileName) return ''
  const dot = fileName.lastIndexOf('.')
  if (dot < 0 || dot === fileName.length - 1) return ''
  return fileName.slice(dot + 1).toUpperCase()
}
