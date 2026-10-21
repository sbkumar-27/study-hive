import { useAuth } from '../context/AuthContext'
import { formatDate } from '../utils/formatDate'

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? '' : ''
  return (first + last).toUpperCase() || '?'
}

export function ProfilePage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="stack">
      <div className="page-header">
        <h1>Profile</h1>
      </div>
      <div className="card" style={{ maxWidth: 480 }}>
        <div className="row" style={{ marginBottom: 20 }}>
          <span className="avatar" style={{ width: 48, height: 48, fontSize: '1.1rem' }} aria-hidden="true">
            {initialsOf(user.displayName)}
          </span>
          <div>
            <h3 style={{ margin: 0 }}>{user.displayName}</h3>
            <span className="badge badge-primary">{user.globalRole}</span>
          </div>
        </div>
        <dl className="stack">
          <div>
            <strong>Username</strong>
            <p className="muted" style={{ margin: 0 }}>{user.username}</p>
          </div>
          <div>
            <strong>Email</strong>
            <p className="muted" style={{ margin: 0 }}>{user.email}</p>
          </div>
          <div>
            <strong>Member since</strong>
            <p className="muted" style={{ margin: 0 }}>{formatDate(user.createdAt)}</p>
          </div>
        </dl>
      </div>
    </div>
  )
}
