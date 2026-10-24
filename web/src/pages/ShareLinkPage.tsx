import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { communitiesApi } from '../api/communities'
import { ErrorBanner } from '../components/ErrorBanner'
import { Icon } from '../components/Icon'
import { Spinner } from '../components/Spinner'

/**
 * Landing page for a community share link (/c/:code). A logged-in user can open the community's
 * documents read-only WITHOUT joining; private communities with a password still ask for it.
 */
export function ShareLinkPage() {
  const { code = '' } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')

  const previewQuery = useQuery({
    queryKey: ['share-link', code],
    queryFn: () => communitiesApi.previewLink(code),
    retry: false,
  })

  const accessMutation = useMutation({
    mutationFn: () => communitiesApi.accessViaLink(code, password || undefined),
    onSuccess: (community) => navigate(`/communities/${community.communityId}`, { replace: true }),
  })

  const joinMutation = useMutation({
    mutationFn: () => communitiesApi.join({ joinCode: code, password: password || undefined }),
    onSuccess: (community) => navigate(`/communities/${community.communityId}`, { replace: true }),
  })

  const preview = previewQuery.data

  // Already a member or already opened this link before: go straight in.
  useEffect(() => {
    if (preview?.hasAccess) navigate(`/communities/${preview.communityId}`, { replace: true })
  }, [preview, navigate])

  if (previewQuery.isLoading || preview?.hasAccess) return <Spinner />
  if (previewQuery.error) {
    return (
      <div className="stack">
        <h1>Link not found</h1>
        <ErrorBanner error={previewQuery.error} />
        <p className="muted">Ask the person who shared it for a fresh link, or check it was copied completely.</p>
      </div>
    )
  }
  if (!preview) return null

  const busy = accessMutation.isPending || joinMutation.isPending
  const needsPassword = preview.requiresPassword && !password

  function handleOpen(e: FormEvent) {
    e.preventDefault()
    accessMutation.mutate()
  }

  return (
    <div className="stack" style={{ maxWidth: 560 }}>
      <p className="text-sm muted" style={{ marginBottom: 0 }}>You've been invited to view</p>
      <h1 style={{ marginBottom: 4 }}>{preview.name}</h1>
      <div className="row text-sm muted">
        <span className="badge">
          <Icon name={preview.communityType === 'PRIVATE' ? 'lock' : 'globe'} size={11} />
          {preview.communityType}
        </span>
        <span>{preview.memberCount} members</span>
        <span>&middot;</span>
        <span>owned by {preview.ownerUsername}</span>
      </div>
      {preview.description && <p>{preview.description}</p>}

      <form className="card form" onSubmit={handleOpen}>
        <ErrorBanner error={accessMutation.error ?? joinMutation.error} />
        {preview.requiresPassword && (
          <div className="form-group">
            <label htmlFor="community-password">Community password</label>
            <input
              id="community-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoFocus
            />
          </div>
        )}
        <button type="submit" className="btn btn-primary" disabled={busy || needsPassword}>
          <Icon name="materials" size={16} />
          {accessMutation.isPending ? 'Opening...' : 'Open documents without joining'}
        </button>
        <button
          type="button"
          className="btn"
          onClick={() => joinMutation.mutate()}
          disabled={busy || needsPassword}
        >
          <Icon name="plus" size={16} />
          {joinMutation.isPending ? 'Joining...' : 'Join community instead'}
        </button>
        <p className="text-sm muted" style={{ margin: 0 }}>
          Without joining you can view and download documents. Join to upload your own and appear in the members
          list.
        </p>
      </form>
    </div>
  )
}
