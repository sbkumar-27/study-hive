import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { communitiesApi, shareLinkFor } from '../api/communities'
import { subjectsApi } from '../api/subjects'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'
import { EmptyState } from '../components/EmptyState'
import { Icon } from '../components/Icon'
import { formatDate } from '../utils/formatDate'

export function CommunityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const communityId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const communityQuery = useQuery({
    queryKey: ['community', communityId],
    queryFn: () => communitiesApi.get(communityId),
  })
  const membersQuery = useQuery({
    queryKey: ['community', communityId, 'members'],
    queryFn: () => communitiesApi.members(communityId),
    enabled: !!communityQuery.data?.currentUserRole,
  })
  const subjectsQuery = useQuery({
    queryKey: ['community', communityId, 'subjects'],
    queryFn: () => subjectsApi.list(communityId),
    enabled: !!communityQuery.data?.currentUserRole || !!communityQuery.data?.guestAccess,
  })

  const [subjectCode, setSubjectCode] = useState('')
  const [subjectName, setSubjectName] = useState('')
  const [subjectError, setSubjectError] = useState<unknown>(null)
  const [showSubjectForm, setShowSubjectForm] = useState(false)
  const [confirmingLeave, setConfirmingLeave] = useState(false)
  const [copied, setCopied] = useState(false)
  const [joinPassword, setJoinPassword] = useState('')

  const addSubject = useMutation({
    mutationFn: () => subjectsApi.create(communityId, { subjectCode, subjectName }),
    onSuccess: () => {
      setSubjectCode('')
      setSubjectName('')
      setShowSubjectForm(false)
      setSubjectError(null)
      queryClient.invalidateQueries({ queryKey: ['community', communityId, 'subjects'] })
    },
    onError: (err) => setSubjectError(err),
  })

  const refreshCommunity = () => {
    queryClient.invalidateQueries({ queryKey: ['community', communityId] })
    queryClient.invalidateQueries({ queryKey: ['communities'] })
  }

  // Public community, no access yet: open its documents read-only without joining.
  const openWithoutJoining = useMutation({
    mutationFn: (code: string) => communitiesApi.accessViaLink(code),
    onSuccess: refreshCommunity,
  })

  // Guest who decides to become a full member.
  const joinMutation = useMutation({
    mutationFn: (code: string) => communitiesApi.join({ joinCode: code, password: joinPassword || undefined }),
    onSuccess: refreshCommunity,
  })

  const leaveMutation = useMutation({
    mutationFn: () => communitiesApi.leave(communityId),
    onSuccess: () => navigate('/communities'),
  })

  if (communityQuery.isLoading) return <Spinner />
  if (communityQuery.error) return <ErrorBanner error={communityQuery.error} />
  const community = communityQuery.data
  if (!community) return null

  const isMember = !!community.currentUserRole
  const isOwner = community.currentUserRole === 'OWNER'
  const isGuest = !isMember && community.guestAccess
  const canView = isMember || isGuest

  async function copyShareLink(code: string) {
    try {
      await navigator.clipboard.writeText(shareLinkFor(code))
    } catch {
      window.prompt('Copy this link:', shareLinkFor(code))
    }
    setCopied(true)
    window.setTimeout(() => setCopied(false), 2000)
  }

  function handleAddSubject(e: FormEvent) {
    e.preventDefault()
    addSubject.mutate()
  }

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1 style={{ marginBottom: 4 }}>{community.name}</h1>
          <div className="row text-sm muted">
            <span className="badge">
              <Icon name={community.communityType === 'PRIVATE' ? 'lock' : 'globe'} size={11} />
              {community.communityType}
            </span>
            <span>{community.memberCount} members</span>
            <span>&middot;</span>
            <span>owned by {community.ownerUsername}</span>
          </div>
        </div>
        {isMember && !isOwner && !confirmingLeave && (
          <div className="actions">
            <button className="btn btn-danger" onClick={() => setConfirmingLeave(true)}>
              Leave community
            </button>
          </div>
        )}
      </div>

      {isMember && !isOwner && confirmingLeave && (
        <div className="confirm-box">
          <Icon name="warning" size={16} />
          <span style={{ flex: 1 }}>Leave <strong>{community.name}</strong>? You'll need the join code to rejoin.</span>
          <button
            className="btn btn-danger btn-sm"
            onClick={() => leaveMutation.mutate()}
            disabled={leaveMutation.isPending}
          >
            {leaveMutation.isPending ? 'Leaving...' : 'Yes, leave'}
          </button>
          <button className="btn btn-sm" onClick={() => setConfirmingLeave(false)} disabled={leaveMutation.isPending}>
            Cancel
          </button>
        </div>
      )}

      {community.description && <p>{community.description}</p>}

      {isMember && community.joinCode && (
        <div className="card row-between">
          <div>
            <strong>Join code:</strong> <code>{community.joinCode}</code>
            {community.hasPassword && (
              <span className="badge badge-warning" style={{ marginLeft: 8 }}>
                <Icon name="lock" size={11} />
                Password protected
              </span>
            )}
            <p className="text-sm muted" style={{ marginTop: 8, marginBottom: 0 }}>
              Share the code so friends can join, or copy the link so anyone with an account can open the
              documents without joining{community.hasPassword ? ' (they will still need the password)' : ''}.
            </p>
            <code className="text-sm" style={{ display: 'block', marginTop: 8, wordBreak: 'break-all' }}>
              {shareLinkFor(community.joinCode)}
            </code>
          </div>
          <button className="btn btn-primary" onClick={() => copyShareLink(community.joinCode!)}>
            <Icon name={copied ? 'check' : 'file'} size={16} />
            {copied ? 'Link copied' : 'Copy share link'}
          </button>
        </div>
      )}

      {isGuest && community.joinCode && (
        <div className="alert alert-info">
          <Icon name="globe" size={16} />
          <span style={{ flex: 1 }}>
            You're viewing this community through a share link. You can view and download documents; join to upload
            your own.
          </span>
          {community.hasPassword && (
            <input
              type="password"
              placeholder="Community password"
              value={joinPassword}
              onChange={(e) => setJoinPassword(e.target.value)}
              style={{ maxWidth: 200 }}
            />
          )}
          <button
            className="btn btn-sm btn-primary"
            onClick={() => joinMutation.mutate(community.joinCode!)}
            disabled={joinMutation.isPending || (community.hasPassword && !joinPassword)}
          >
            {joinMutation.isPending ? 'Joining...' : 'Join community'}
          </button>
        </div>
      )}
      {joinMutation.error && <ErrorBanner error={joinMutation.error} />}

      {!canView && (
        <div className="alert alert-info">
          <Icon name="warning" size={16} />
          <span style={{ flex: 1 }}>
            You're viewing limited public information. Open the documents without joining, or join this community
            (using its join code from the Communities page) to upload.
          </span>
          {community.joinCode && (
            <button
              className="btn btn-sm btn-primary"
              onClick={() => openWithoutJoining.mutate(community.joinCode!)}
              disabled={openWithoutJoining.isPending}
            >
              {openWithoutJoining.isPending ? 'Opening...' : 'Open documents'}
            </button>
          )}
        </div>
      )}
      {openWithoutJoining.error && <ErrorBanner error={openWithoutJoining.error} />}

      {canView && (
        <>
          <div className="row-between">
            <h2 style={{ marginBottom: 0 }}>Subjects</h2>
            {isMember && (
            <button className="btn btn-sm" onClick={() => setShowSubjectForm((v) => !v)}>
              {showSubjectForm ? (
                'Cancel'
              ) : (
                <>
                  <Icon name="plus" size={14} />
                  Add subject
                </>
              )}
            </button>
            )}
          </div>

          {showSubjectForm && (
            <form className="row card" onSubmit={handleAddSubject}>
              <input
                placeholder="Code (e.g. CS301)"
                value={subjectCode}
                onChange={(e) => setSubjectCode(e.target.value)}
                style={{ maxWidth: 160 }}
                required
              />
              <input
                placeholder="Name (e.g. Data Structures)"
                value={subjectName}
                onChange={(e) => setSubjectName(e.target.value)}
                style={{ maxWidth: 260 }}
                required
              />
              <button className="btn btn-primary" type="submit" disabled={addSubject.isPending}>
                Add
              </button>
            </form>
          )}
          <ErrorBanner error={subjectError} />

          {subjectsQuery.isLoading && <Spinner />}
          {subjectsQuery.data && subjectsQuery.data.length === 0 && (
            <EmptyState
              icon="materials"
              title="No subjects yet"
              hint={isMember ? 'Add the first subject for this community.' : 'Nothing has been added here yet.'}
            />
          )}
          {subjectsQuery.data && subjectsQuery.data.length > 0 && (
            <div className="grid">
              {subjectsQuery.data.map((s) => (
                <Link
                  key={s.subjectId}
                  to={`/materials?communityId=${communityId}&subjectId=${s.subjectId}`}
                  className="card card-link"
                >
                  <div className="row" style={{ alignItems: 'flex-start' }}>
                    <span className="card-icon">
                      <Icon name="materials" size={16} />
                    </span>
                    <div>
                      <strong>{s.subjectCode}</strong>
                      <p className="text-sm muted" style={{ marginBottom: 0 }}>
                        {s.subjectName}
                      </p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}

          <div className="row">
            <Link to={`/materials?communityId=${communityId}`} className="btn btn-primary">
              <Icon name="materials" size={16} />
              Browse all materials
            </Link>
            {isMember && (
              <Link to={`/materials/new?communityId=${communityId}`} className="btn">
                <Icon name="upload" size={16} />
                Upload material
              </Link>
            )}
          </div>
        </>
      )}

      {isMember && (
        <>
          <h2>Members</h2>
          {membersQuery.isLoading && <Spinner />}
          {membersQuery.data && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {membersQuery.data.map((m) => (
                    <tr key={m.userId}>
                      <td>{m.displayName}</td>
                      <td>{m.username}</td>
                      <td>
                        <span className="badge badge-primary">{m.memberRole}</span>
                      </td>
                      <td className="text-sm muted">{formatDate(m.joinedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {leaveMutation.error && <ErrorBanner error={leaveMutation.error} />}
    </div>
  )
}
