import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { communitiesApi } from '../api/communities'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import type { Community } from '../types'

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
      <div className="row-between">
        <h1>Communities</h1>
        <Link to="/communities/new" className="btn btn-primary">
          Create community
        </Link>
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

      <div className="row">
        <button
          className={`btn btn-sm ${tab === 'browse' ? 'btn-primary' : ''}`}
          onClick={() => switchTab('browse')}
        >
          Browse public
        </button>
        <button className={`btn btn-sm ${tab === 'mine' ? 'btn-primary' : ''}`} onClick={() => switchTab('mine')}>
          My communities
        </button>
      </div>

      {tab === 'browse' && (
        <input
          placeholder="Search communities by name..."
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
          style={{ maxWidth: 360 }}
        />
      )}

      <ErrorBanner error={query.error} />

      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState
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
      <div className="row-between" style={{ marginBottom: 8 }}>
        <h3 style={{ margin: 0 }}>{community.name}</h3>
        {community.communityType === 'PRIVATE' && <span className="badge">Private</span>}
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
