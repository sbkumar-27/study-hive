export function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="empty-state">
      <p style={{ fontWeight: 600, marginBottom: hint ? 4 : 0 }}>{title}</p>
      {hint && <p className="text-sm">{hint}</p>}
    </div>
  )
}
