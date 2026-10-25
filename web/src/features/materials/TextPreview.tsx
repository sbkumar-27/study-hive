interface TextPreviewProps {
  text: string
}

export function TextPreview({ text }: TextPreviewProps) {
  return <pre className="viewer-text">{text}</pre>
}
