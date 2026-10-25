import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { communitiesApi } from '../../features/communities/communities'
import { materialsApi } from '../../features/materials/materials'
import { Spinner } from './Spinner'
import { EmptyState } from './EmptyState'
import { formatDate } from '../utils/formatDate'

const FEED_SIZE = 12

export function ActivityFeed() {
  const communitiesQuery = useQuery({
    queryKey: ['communities', 'mine', 'activity'],
    queryFn: () => communitiesApi.mine(0, 100),
  })
  const materialsQuery = useQuery({
    queryKey: ['materials', 'activity-feed'],
    queryFn: () => materialsApi.search({ sort: 'newest', size: 50 }),
  })

  const isLoading = communitiesQuery.isLoading || materialsQuery.isLoading
  const error = communitiesQuery.error ?? materialsQuery.error

  const communityNameById = new Map<number, string>()
  for (const c of communitiesQuery.data?.content ?? []) {
    communityNameById.set(c.communityId, c.name)
  }

  const rows = (materialsQuery.data?.content ?? [])
    .filter((m) => m.communityId !== null && communityNameById.has(m.communityId))
    .slice(0, FEED_SIZE)

  return (
    <div className="card">
      <h3 style={{ marginBottom: 10 }}>Recent activity</h3>

      {isLoading && <Spinner />}
      {!isLoading && error && <p className="text-sm muted">Couldn't load recent activity.</p>}

      {!isLoading && !error && rows.length === 0 && (
        <EmptyState title="No activity yet" hint="Uploads to your communities will show up here." />
      )}

      {!isLoading && !error && rows.length > 0 && (
        <div className="activity-feed">
          {rows.map((m) => (
            <div key={m.materialId} className="activity-row">
              <span>
                <strong>{m.uploadedByUsername}</strong> uploaded{' '}
                <Link to={`/materials/${m.materialId}`}>{m.title}</Link> to{' '}
                <Link to={`/communities/${m.communityId}`}>{communityNameById.get(m.communityId as number)}</Link>
              </span>
              <span className="activity-time">{formatDate(m.createdAt)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
