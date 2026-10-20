import { useAuth } from '../context/AuthContext'

export function ProfilePage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="stack">
      <h1>Profile</h1>
      <div className="card" style={{ maxWidth: 480 }}>
        <dl className="stack">
          <div>
            <strong>Display name</strong>
            <p className="muted" style={{ margin: 0 }}>{user.displayName}</p>
          </div>
          <div>
            <strong>Username</strong>
            <p className="muted" style={{ margin: 0 }}>{user.username}</p>
          </div>
          <div>
            <strong>Email</strong>
            <p className="muted" style={{ margin: 0 }}>{user.email}</p>
          </div>
          <div>
            <strong>Role</strong>
            <p className="muted" style={{ margin: 0 }}>{user.globalRole}</p>
          </div>
          <div>
            <strong>Member since</strong>
            <p className="muted" style={{ margin: 0 }}>{new Date(user.createdAt).toLocaleDateString()}</p>
          </div>
        </dl>
      </div>
    </div>
  )
}
