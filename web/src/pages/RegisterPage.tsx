import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ErrorBanner } from '../components/ErrorBanner'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ username: '', email: '', password: '', displayName: '' })
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register(form)
      const redirectTo = (location.state as { from?: string } | null)?.from ?? '/communities'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="card auth-card" style={{ maxWidth: 440 }}>
        <div className="auth-brand">
          <span className="navbar-logo">SH</span>
          <strong>StudyHive</strong>
        </div>
        <h1 style={{ marginBottom: 4 }}>Create your account</h1>
        <p className="page-subtitle" style={{ marginBottom: 20 }}>Join StudyHive to share and discover study materials.</p>
        <form className="form" onSubmit={handleSubmit}>
          <ErrorBanner error={error} />
          <div className="form-group">
            <label htmlFor="displayName">Display name</label>
            <input
              id="displayName"
              value={form.displayName}
              onChange={(e) => update('displayName', e.target.value)}
              required
              autoFocus
            />
          </div>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              value={form.username}
              onChange={(e) => update('username', e.target.value)}
              required
              minLength={3}
              pattern="[a-zA-Z0-9_.\-]+"
              title="Letters, numbers, dots, dashes, or underscores only"
            />
          </div>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={form.email}
              onChange={(e) => update('email', e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={form.password}
              onChange={(e) => update('password', e.target.value)}
              required
              minLength={8}
            />
            <span className="hint">At least 8 characters.</span>
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>
        <p className="text-sm muted" style={{ marginTop: 16 }}>
          Already have an account? <Link to="/login" state={location.state}>Log in</Link>
        </p>
      </div>
    </div>
  )
}
