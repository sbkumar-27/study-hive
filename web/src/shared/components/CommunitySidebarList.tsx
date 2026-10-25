import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { communitiesApi } from '../../features/communities/communities'
import { Icon } from './Icon'
import { Spinner } from './Spinner'
import { EmptyState } from './EmptyState'

export function CommunitySidebarList() {
  const query = useQuery({ queryKey: ['communities', 'mine', 'sidebar'], queryFn: () => communitiesApi.mine(0, 50) })

  return (
    <div className="card">
      <div className="row-between" style={{ marginBottom: 10 }}>
        <h3 style={{ margin: 0 }}>Your communities</h3>
        <Link to="/communities" className="text-sm btn-link">
          Browse
        </Link>
      </div>

      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState
          icon="communities"
          title="No communities yet"
          hint="Join one with a code or create your own."
          action={
            <Link to="/communities" className="btn btn-primary btn-sm">
              Browse communities
            </Link>
          }
        />
      )}

      {query.data && query.data.content.length > 0 && (
        <div className="sidebar-list">
          {query.data.content.map((c) => (
            <Link key={c.communityId} to={`/communities/${c.communityId}`} className="sidebar-list-item">
              <span className="row" style={{ gap: 8, minWidth: 0 }}>
                <Icon name={c.communityType === 'PRIVATE' ? 'lock' : 'globe'} size={14} />
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.name}</span>
              </span>
              <span className="text-sm muted">{c.memberCount}</span>
            </Link>
          ))}
        </div>
      )}

      <Link to="/communities/new" className="btn btn-sm" style={{ marginTop: 12, width: '100%' }}>
        <Icon name="plus" size={14} />
        Create community
      </Link>
    </div>
  )
}
