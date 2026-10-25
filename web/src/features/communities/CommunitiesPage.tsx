import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { communitiesApi } from './communities'
import { ErrorBanner } from '../../shared/components/ErrorBanner'
import { EmptyState } from '../../shared/components/EmptyState'
import { Icon } from '../../shared/components/Icon'
import { Pagination } from '../../shared/components/Pagination'
import { Spinner } from '../../shared/components/Spinner'
import type { Community } from '../../shared/types'

type Tab = 'browse' | 'mine'

export function CommunitiesPage() {
  const [tab, setTab] = useState<Tab>('browse')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [joinCode, setJoinCode] = useState('')
  const [joinPassword, setJoinPassword] = useState('')
  const [joinError, setJoinError] = useState<unknown>(null)
  const [joining, setJoining] = useState(false)
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['communities', tab, q, page],
    queryFn: () => (tab === 'browse' ? communitiesApi.search(q || undefined, page) : communitiesApi.mine(page)),
  })

  function switchTab(next: Tab) {
    setTab(next)
    setPage(0)
  }

  async function handleJoin(e: FormEvent) {
    e.preventDefault()
    setJoinError(null)
    setJoining(true)
    try {
      await communitiesApi.join({ joinCode: joinCode.trim(), password: joinPassword || undefined })
      setJoinCode('')
      setJoinPassword('')
      await queryClient.invalidateQueries({ queryKey: ['communities'] })
      switchTab('mine')
    } catch (err) {
      setJoinError(err)
    } finally {
      setJoining(false)
    }
  }

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Communities</h1>
          <p className="page-subtitle">Browse communities, share notes, and find the materials you need.</p>
        </div>
        <div className="actions">
          <Link to="/communities/new" className="btn btn-primary">
            <Icon name="plus" size={16} />
            Create community
          </Link>
        </div>
      </div>

      <div className="card">
        <h3>Have a join code?</h3>
        <form className="row" onSubmit={handleJoin}>
          <input
            placeholder="Join code"
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value)}
            style={{ maxWidth: 180 }}
            required
          />
          <input
            placeholder="Password (if required)"
            type="password"
            value={joinPassword}
            onChange={(e) => setJoinPassword(e.target.value)}
            style={{ maxWidth: 220 }}
          />
          <button className="btn btn-primary" disabled={joining} type="submit">
            {joining ? 'Joining...' : 'Join'}
          </button>
        </form>
        <ErrorBanner error={joinError} />
      </div>

      <div className="row-between">
        <div className="tabs" role="tablist" aria-label="Community lists">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'browse'}
            className={`tab-btn ${tab === 'browse' ? 'active' : ''}`}
            onClick={() => switchTab('browse')}
          >
            Browse public
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'mine'}
            className={`tab-btn ${tab === 'mine' ? 'active' : ''}`}
            onClick={() => switchTab('mine')}
          >
            My communities
          </button>
        </div>

        {tab === 'browse' && (
          <div style={{ position: 'relative', maxWidth: 320, width: '100%' }}>
            <span
              style={{
                position: 'absolute',
                left: 10,
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--color-text-faint)',
                pointerEvents: 'none',
              }}
            >
              <Icon name="search" size={16} />
            </span>
            <input
              placeholder="Search communities by name..."
              value={q}
              onChange={(e) => {
                setQ(e.target.value)
                setPage(0)
              }}
              style={{ paddingLeft: 34 }}
            />
          </div>
        )}
      </div>

      <ErrorBanner error={query.error} />

      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState
          icon="communities"
          title={tab === 'browse' ? 'No public communities found' : "You haven't joined any communities yet"}
          hint={tab === 'browse' ? 'Try a different search, or create your own.' : 'Browse public communities or join one with a code.'}
        />
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="grid">
            {query.data.content.map((c) => (
              <CommunityCard key={c.communityId} community={c} />
            ))}
          </div>
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

function CommunityCard({ community }: { community: Community }) {
  return (
    <Link to={`/communities/${community.communityId}`} className="card card-link">
      <div className="row" style={{ marginBottom: 10, alignItems: 'flex-start' }}>
        <span className="card-icon">
          <Icon name={community.communityType === 'PRIVATE' ? 'lock' : 'globe'} size={18} />
        </span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="row-between" style={{ gap: 8 }}>
            <h3 style={{ margin: 0 }}>{community.name}</h3>
            {community.communityType === 'PRIVATE' ? (
              <span className="badge badge-warning">
                <Icon name="lock" size={11} />
                Private
              </span>
            ) : (
              <span className="badge">
                <Icon name="globe" size={11} />
                Public
              </span>
            )}
          </div>
        </div>
      </div>
      {community.description && <p className="text-sm muted">{community.description}</p>}
      <div className="row text-sm muted">
        <span>{community.memberCount} members</span>
        <span>&middot;</span>
        <span>by {community.ownerUsername}</span>
        {community.currentUserRole && (
          <>
            <span>&middot;</span>
            <span className="badge badge-primary">{community.currentUserRole}</span>
          </>
        )}
      </div>
    </Link>
  )
}
