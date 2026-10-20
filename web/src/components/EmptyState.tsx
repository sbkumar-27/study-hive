import type { ReactNode } from 'react'
import { Icon, type IconName } from './Icon'

export function EmptyState({
  title,
  hint,
  icon = 'empty',
  action,
}: {
  title: string
  hint?: string
  icon?: IconName
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">
        <Icon name={icon} size={32} />
      </div>
      <p style={{ fontWeight: 600, marginBottom: hint ? 4 : 0, color: 'var(--color-text)' }}>{title}</p>
      {hint && <p className="text-sm" style={{ marginBottom: action ? 12 : 0 }}>{hint}</p>}
      {action}
    </div>
  )
}
