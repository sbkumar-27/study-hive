import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { communitiesApi } from '../api/communities'
import { subjectsApi } from '../api/subjects'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'
import { EmptyState } from '../components/EmptyState'

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
    enabled: !!communityQuery.data?.currentUserRole,
  })

  const [subjectCode, setSubjectCode] = useState('')
  const [subjectName, setSubjectName] = useState('')
  const [subjectError, setSubjectError] = useState<unknown>(null)
  const [showSubjectForm, setShowSubjectForm] = useState(false)

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

  function handleAddSubject(e: FormEvent) {
    e.preventDefault()
    addSubject.mutate()
  }

  return (
    <div className="stack">
      <div className="row-between">
        <div>
          <h1 style={{ marginBottom: 4 }}>{community.name}</h1>
          <div className="row text-sm muted">
            <span className="badge">{community.communityType}</span>
            <span>{community.memberCount} members</span>
            <span>&middot;</span>
            <span>owned by {community.ownerUsername}</span>
          </div>
        </div>
        {isMember && !isOwner && (
          <button className="btn btn-danger" onClick={() => leaveMutation.mutate()} disabled={leaveMutation.isPending}>
            Leave community
          </button>
        )}
      </div>

      {community.description && <p>{community.description}</p>}

      {isMember && community.joinCode && (
        <div className="card">
          <strong>Join code:</strong> <code>{community.joinCode}</code>
          {community.hasPassword && <span className="badge badge-warning" style={{ marginLeft: 8 }}>Password protected</span>}
          <p className="text-sm muted" style={{ marginTop: 8, marginBottom: 0 }}>
            Share this code with friends so they can join.
          </p>
        </div>
      )}

      {!isMember && (
        <div className="alert alert-error">
          You're viewing limited public information. Join this community (using its join code from the
          Communities page) to see subjects, members, and materials.
        </div>
      )}

      {isMember && (
        <>
          <div className="row-between">
            <h2 style={{ marginBottom: 0 }}>Subjects</h2>
            <button className="btn btn-sm" onClick={() => setShowSubjectForm((v) => !v)}>
              {showSubjectForm ? 'Cancel' : 'Add subject'}
            </button>
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
            <EmptyState title="No subjects yet" hint="Add the first subject for this community." />
          )}
          {subjectsQuery.data && subjectsQuery.data.length > 0 && (
            <div className="grid">
              {subjectsQuery.data.map((s) => (
                <Link
                  key={s.subjectId}
                  to={`/materials?communityId=${communityId}&subjectId=${s.subjectId}`}
                  className="card card-link"
                >
                  <strong>{s.subjectCode}</strong>
                  <p className="text-sm muted" style={{ marginBottom: 0 }}>
                    {s.subjectName}
                  </p>
                </Link>
              ))}
            </div>
          )}

          <div className="row">
            <Link to={`/materials?communityId=${communityId}`} className="btn btn-primary">
              Browse all materials
            </Link>
            <Link to={`/materials/new?communityId=${communityId}`} className="btn">
              Upload material
            </Link>
          </div>

          <h2>Members</h2>
          {membersQuery.isLoading && <Spinner />}
          {membersQuery.data && (
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
                    <td className="text-sm muted">{new Date(m.joinedAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {leaveMutation.error && <ErrorBanner error={leaveMutation.error} />}
    </div>
  )
}
