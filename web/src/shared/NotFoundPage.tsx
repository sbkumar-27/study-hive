import { Link } from 'react-router-dom'
import { Icon } from './components/Icon'

export function NotFoundPage() {
  return (
    <div className="center-pad" style={{ minHeight: '100vh', alignItems: 'center' }}>
      <div className="empty-state" style={{ maxWidth: 420 }}>
        <div className="empty-icon">
          <Icon name="warning" size={32} />
        </div>
        <h2>Page not found</h2>
        <p className="text-sm">The page you're looking for doesn't exist or may have moved.</p>
        <Link to="/communities" className="btn btn-primary">
          Back to Communities
        </Link>
      </div>
    </div>
  )
}
