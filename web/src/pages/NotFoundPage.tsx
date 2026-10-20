import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="empty-state">
      <h2>Page not found</h2>
      <p>
        <Link to="/communities">Back to Communities</Link>
      </p>
    </div>
  )
}
