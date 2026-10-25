import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { communitiesApi, shareLinkFor } from './communities'
import { subjectsApi } from './subjects'
import { materialsApi } from '../materials/materials'
import { questionPapersApi } from '../questionpapers/questionPapers'
import { reportsApi } from '../reports/reports'
import { ErrorBanner } from '../../shared/components/ErrorBanner'
import { Spinner } from '../../shared/components/Spinner'
import { EmptyState } from '../../shared/components/EmptyState'
import { Icon } from '../../shared/components/Icon'
import { Pagination } from '../../shared/components/Pagination'
import { formatDate } from '../../shared/utils/formatDate'
import { materialTypeBadgeClass } from '../../shared/utils/materialType'
import { useAuth } from '../auth/AuthContext'
import type { Community, Material, ReportStatus, Subject } from '../../shared/types'

type TabKey = 'materials' | 'papers' | 'subjects' | 'members' | 'reports'

function getStarredIds(userId: number): number[] {
  try {
    const raw = localStorage.getItem(`studyhive.starred.${userId}`)
    return raw ? (JSON.parse(raw) as number[]) : []
  } catch {
    return []
  }
}

function saveStarredIds(userId: number, ids: number[]) {
  try {
    localStorage.setItem(`studyhive.starred.${userId}`, JSON.stringify(ids))
  } catch {
    // localStorage unavailable -- starring is a client-side convenience only, safe to skip.
  }
}

export function CommunityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const communityId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()

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
  const statsQuery = useQuery({
    queryKey: ['community', communityId, 'stats'],
    queryFn: () => communitiesApi.stats(communityId),
    enabled: !!communityQuery.data?.currentUserRole || !!communityQuery.data?.guestAccess,
  })

  const [activeTab, setActiveTab] = useState<TabKey>('materials')
  const [subjectCode, setSubjectCode] = useState('')
  const [subjectName, setSubjectName] = useState('')
  const [subjectError, setSubjectError] = useState<unknown>(null)
  const [showSubjectForm, setShowSubjectForm] = useState(false)
  const [confirmingLeave, setConfirmingLeave] = useState(false)
  const [copied, setCopied] = useState(false)
  const [joinPassword, setJoinPassword] = useState('')
  const [starred, setStarred] = useState<number[]>(() => (user ? getStarredIds(user.userId) : []))
  const [editingNote, setEditingNote] = useState(false)
  const [noteDraft, setNoteDraft] = useState('')
  const [materialsSubjectFilter, setMaterialsSubjectFilter] = useState('')

  useEffect(() => {
    if (user) setStarred(getStarredIds(user.userId))
  }, [user])

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

  const pinnedNoteMutation = useMutation({
    mutationFn: (note: string | null) => communitiesApi.updatePinnedNote(communityId, note),
    onSuccess: () => {
      setEditingNote(false)
      queryClient.invalidateQueries({ queryKey: ['community', communityId] })
    },
  })

  if (communityQuery.isLoading) return <Spinner />
  if (communityQuery.error) return <ErrorBanner error={communityQuery.error} />
  const community = communityQuery.data
  if (!community) return null

  const isMember = !!community.currentUserRole
  const isOwner = community.currentUserRole === 'OWNER'
  const isGuest = !isMember && community.guestAccess
  const canView = isMember || isGuest
  const isStarred = user ? starred.includes(communityId) : false

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

  function toggleStar() {
    if (!user) return
    const next = isStarred ? starred.filter((sid) => sid !== communityId) : [...starred, communityId]
    saveStarredIds(user.userId, next)
    setStarred(next)
  }

  function openSubjectInMaterials(subjectId: number) {
    setMaterialsSubjectFilter(String(subjectId))
    setActiveTab('materials')
  }

  const tabs: { key: TabKey; label: string }[] = [
    { key: 'materials', label: 'Materials' },
    { key: 'papers', label: 'Question papers' },
    { key: 'subjects', label: 'Subjects' },
    { key: 'members', label: 'Members' },
    ...(isOwner ? ([{ key: 'reports', label: 'Reports' }] as { key: TabKey; label: string }[]) : []),
  ]

  return (
    <div className="stack">
      <div className="community-header">
        <div>
          <h1 className="community-title">
            <span className="owner-part">{community.ownerUsername} /</span> {community.name}
          </h1>
          <div className="row text-sm muted" style={{ marginTop: 4 }}>
            <span className="badge">
              <Icon name={community.communityType === 'PRIVATE' ? 'lock' : 'globe'} size={11} />
              {community.communityType}
            </span>
            <span>{community.memberCount} members</span>
          </div>
        </div>
        <div className="row">
          {user && (
            <button className="btn btn-sm" onClick={toggleStar}>
              <Icon
                name={isStarred ? 'starFilled' : 'star'}
                size={15}
                style={isStarred ? { color: 'var(--color-accent)' } : undefined}
              />
              {isStarred ? 'Starred' : 'Star'}
            </button>
          )}
          {isMember && community.joinCode && (
            <button className="btn btn-sm" onClick={() => copyShareLink(community.joinCode!)}>
              <Icon name={copied ? 'check' : 'file'} size={14} />
              {copied ? 'Link copied' : 'Share link'}
            </button>
          )}
          {isMember && !isOwner && !confirmingLeave && (
            <button className="btn btn-sm btn-danger" onClick={() => setConfirmingLeave(true)}>
              Leave
            </button>
          )}
        </div>
      </div>

      {isMember && !isOwner && confirmingLeave && (
        <div className="confirm-box">
          <Icon name="warning" size={16} />
          <span style={{ flex: 1 }}>
            Leave <strong>{community.name}</strong>? You'll need the join code to rejoin.
          </span>
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
      {leaveMutation.error && <ErrorBanner error={leaveMutation.error} />}

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

      {community.description && <p>{community.description}</p>}

      {canView && (
        <>
          {community.pinnedNote && !editingNote && (
            <div className="pinned-note">
              <Icon name="warning" size={16} />
              <div style={{ flex: 1 }}>
                <strong className="text-sm">Pinned note</strong>
                <p style={{ margin: '4px 0 0' }}>{community.pinnedNote}</p>
              </div>
              {isOwner && (
                <button
                  className="btn btn-sm"
                  onClick={() => {
                    setNoteDraft(community.pinnedNote ?? '')
                    setEditingNote(true)
                  }}
                >
                  Edit
                </button>
              )}
            </div>
          )}
          {isOwner && !community.pinnedNote && !editingNote && (
            <button
              className="btn btn-sm"
              style={{ width: 'fit-content' }}
              onClick={() => {
                setNoteDraft('')
                setEditingNote(true)
              }}
            >
              <Icon name="plus" size={14} />
              Add pinned note
            </button>
          )}
          {isOwner && editingNote && (
            <div className="card stack">
              <ErrorBanner error={pinnedNoteMutation.error} />
              <textarea
                value={noteDraft}
                onChange={(e) => setNoteDraft(e.target.value)}
                placeholder="Pin an announcement for this community..."
                autoFocus
              />
              <div className="row">
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => pinnedNoteMutation.mutate(noteDraft.trim() ? noteDraft : null)}
                  disabled={pinnedNoteMutation.isPending}
                >
                  Save
                </button>
                {community.pinnedNote && (
                  <button
                    className="btn btn-sm"
                    onClick={() => pinnedNoteMutation.mutate(null)}
                    disabled={pinnedNoteMutation.isPending}
                  >
                    Clear
                  </button>
                )}
                <button className="btn btn-sm" onClick={() => setEditingNote(false)}>
                  Cancel
                </button>
              </div>
            </div>
          )}

          <div className="community-layout">
            <div className="stack">
              <div className="tabs" role="tablist" aria-label="Community sections">
                {tabs.map((t) => (
                  <button
                    key={t.key}
                    type="button"
                    role="tab"
                    aria-selected={activeTab === t.key}
                    className={`tab-btn ${activeTab === t.key ? 'active' : ''}`}
                    onClick={() => setActiveTab(t.key)}
                  >
                    {t.label}
                  </button>
                ))}
              </div>

              {activeTab === 'materials' && (
                <MaterialsTab
                  communityId={communityId}
                  isMember={isMember}
                  subjects={subjectsQuery.data ?? []}
                  subjectFilter={materialsSubjectFilter}
                  onSubjectFilterChange={setMaterialsSubjectFilter}
                />
              )}

              {activeTab === 'papers' && <PapersTab communityId={communityId} />}

              {activeTab === 'subjects' && (
                <div className="stack">
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
                        <button
                          key={s.subjectId}
                          onClick={() => openSubjectInMaterials(s.subjectId)}
                          className="card card-link"
                          style={{ textAlign: 'left', cursor: 'pointer', border: 'var(--border-width) solid var(--color-border)' }}
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
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {activeTab === 'members' && (
                <MembersTab communityId={communityId} members={membersQuery.data} isOwner={isOwner} loading={membersQuery.isLoading} />
              )}

              {activeTab === 'reports' && isOwner && <ReportsTab />}
            </div>

            <AboutPanel community={community} subjects={subjectsQuery.data ?? []} stats={statsQuery.data} isMember={isMember} copied={copied} onCopyLink={copyShareLink} />
          </div>
        </>
      )}
    </div>
  )
}

function AboutPanel({
  community,
  subjects,
  stats,
  isMember,
  copied,
  onCopyLink,
}: {
  community: Community
  subjects: Subject[]
  stats: { totalViews: number; topContributors: { userId: number; username: string; uploadCount: number }[] } | undefined
  isMember: boolean
  copied: boolean
  onCopyLink: (code: string) => void
}) {
  return (
    <aside className="card about-panel">
      <h3 style={{ margin: 0 }}>About</h3>
      {community.description && <p className="text-sm muted" style={{ marginBottom: 0 }}>{community.description}</p>}

      <div className="stack" style={{ gap: 6 }}>
        <div className="row-between text-sm">
          <span className="muted">Members</span>
          <strong>{community.memberCount}</strong>
        </div>
        <div className="row-between text-sm">
          <span className="muted">Total views</span>
          <strong>{stats ? stats.totalViews : '--'}</strong>
        </div>
        {isMember && community.joinCode && (
          <div className="row-between text-sm">
            <span className="muted">Join code</span>
            <code>{community.joinCode}</code>
          </div>
        )}
      </div>

      {subjects.length > 0 && (
        <div>
          <p className="text-sm muted" style={{ marginBottom: 6 }}>
            Subjects
          </p>
          <div className="row" style={{ gap: 6 }}>
            {subjects.map((s) => (
              <span key={s.subjectId} className="badge">
                {s.subjectCode}
              </span>
            ))}
          </div>
        </div>
      )}

      {stats && stats.topContributors.length > 0 && (
        <div>
          <p className="text-sm muted" style={{ marginBottom: 6 }}>
            Top contributors
          </p>
          <div className="stack" style={{ gap: 6 }}>
            {stats.topContributors.map((c) => (
              <div key={c.userId} className="row-between text-sm">
                <span>{c.username}</span>
                <span className="muted">{c.uploadCount} uploads</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {isMember && community.joinCode && (
        <button className="btn btn-sm" onClick={() => onCopyLink(community.joinCode!)}>
          <Icon name={copied ? 'check' : 'file'} size={14} />
          {copied ? 'Link copied' : 'Copy share link'}
        </button>
      )}
    </aside>
  )
}

function MaterialsTab({
  communityId,
  isMember,
  subjects,
  subjectFilter,
  onSubjectFilterChange,
}: {
  communityId: number
  isMember: boolean
  subjects: Subject[]
  subjectFilter: string
  onSubjectFilterChange: (value: string) => void
}) {
  const [goToFile, setGoToFile] = useState('')
  const [expanded, setExpanded] = useState<Set<string>>(new Set())

  const materialsQuery = useQuery({
    queryKey: ['community', communityId, 'materials'],
    queryFn: () => materialsApi.search({ communityId, size: 200, sort: 'newest' }),
  })

  const allMaterials = materialsQuery.data?.content ?? []
  const latestUpload = allMaterials[0]

  const filtered = allMaterials.filter((m) => {
    if (subjectFilter && String(m.subjectId) !== subjectFilter) return false
    if (goToFile && !m.title.toLowerCase().includes(goToFile.toLowerCase())) return false
    return true
  })

  const groups = useMemo(() => {
    const map = new Map<string, { label: string; items: Material[] }>()
    for (const m of filtered) {
      const key = m.subjectId ? String(m.subjectId) : 'unfiled'
      const label = m.subjectId ? `${m.subjectCode} -- ${m.subjectName}` : 'Unfiled'
      if (!map.has(key)) map.set(key, { label, items: [] })
      map.get(key)!.items.push(m)
    }
    return Array.from(map.entries())
  }, [filtered])

  const forceExpand = goToFile.trim() !== '' || subjectFilter !== ''

  function toggleGroup(key: string) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  return (
    <div className="stack">
      <div className="row-between">
        <div className="row" style={{ gap: 8 }}>
          <select value={subjectFilter} onChange={(e) => onSubjectFilterChange(e.target.value)} style={{ minWidth: 180 }}>
            <option value="">All subjects</option>
            {subjects.map((s) => (
              <option key={s.subjectId} value={String(s.subjectId)}>
                {s.subjectCode} -- {s.subjectName}
              </option>
            ))}
          </select>
          <input placeholder="Go to file..." value={goToFile} onChange={(e) => setGoToFile(e.target.value)} style={{ maxWidth: 200 }} />
        </div>
        {isMember && (
          <Link to={`/materials/new?communityId=${communityId}`} className="btn btn-primary btn-sm">
            <Icon name="upload" size={14} />
            Upload
          </Link>
        )}
      </div>

      {latestUpload && (
        <div className="latest-upload-strip">
          <Icon name="file" size={16} />
          <span>
            Latest upload: <Link to={`/materials/${latestUpload.materialId}`}>{latestUpload.title}</Link> by{' '}
            {latestUpload.uploadedByUsername}
          </span>
          <span className="text-sm muted" style={{ marginLeft: 'auto' }}>
            {formatDate(latestUpload.createdAt)}
          </span>
        </div>
      )}

      {materialsQuery.isLoading && <Spinner />}
      <ErrorBanner error={materialsQuery.error} />

      {!materialsQuery.isLoading && groups.length === 0 && (
        <EmptyState icon="materials" title="No materials found" hint="Try a different subject filter or search." />
      )}

      {groups.length > 0 && (
        <div className="table-wrap">
          {groups.map(([key, group]) => {
            const isOpen = forceExpand || expanded.has(key)
            return (
              <div key={key}>
                <div className="subject-folder-row" onClick={() => toggleGroup(key)}>
                  <Icon name={isOpen ? 'close' : 'materials'} size={14} />
                  <span style={{ flex: 1 }}>{group.label}</span>
                  <span className="text-sm muted">
                    {group.items.length} file{group.items.length === 1 ? '' : 's'}
                  </span>
                </div>
                {isOpen && group.items.map((m) => <MaterialFileRow key={m.materialId} material={m} />)}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function MaterialFileRow({ material }: { material: Material }) {
  return (
    <Link to={`/materials/${material.materialId}`} className="file-row">
      <Icon name={material.materialType === 'QUESTION_PAPER' ? 'papers' : 'file'} size={16} />
      <span className="file-row-title">{material.title}</span>
      <span className={materialTypeBadgeClass(material.materialType)}>{material.materialType.replace('_', ' ')}</span>
      {material.favorited && <Icon name="starFilled" size={14} style={{ color: 'var(--color-accent)' }} />}
      <span className="text-sm muted">{material.uploadedByUsername}</span>
      <span className="text-sm muted">{formatDate(material.createdAt)}</span>
    </Link>
  )
}

function PapersTab({ communityId }: { communityId: number }) {
  const [subjectId, setSubjectId] = useState('')
  const [examYear, setExamYear] = useState('')
  const [examType, setExamType] = useState('')
  const [semester, setSemester] = useState('')
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['community', communityId, 'question-papers', subjectId, examYear, examType, semester, page],
    queryFn: () =>
      questionPapersApi.search({
        communityId,
        subjectId: subjectId ? Number(subjectId) : undefined,
        examYear: examYear ? Number(examYear) : undefined,
        examType: examType || undefined,
        semester: semester ? Number(semester) : undefined,
        page,
      }),
  })

  return (
    <div className="stack">
      <div className="filters">
        <input
          placeholder="Subject ID"
          type="number"
          value={subjectId}
          onChange={(e) => {
            setSubjectId(e.target.value)
            setPage(0)
          }}
        />
        <input
          placeholder="Exam year"
          type="number"
          value={examYear}
          onChange={(e) => {
            setExamYear(e.target.value)
            setPage(0)
          }}
        />
        <input
          placeholder="Exam type (e.g. END_SEM)"
          value={examType}
          onChange={(e) => {
            setExamType(e.target.value)
            setPage(0)
          }}
        />
        <input
          placeholder="Semester"
          type="number"
          value={semester}
          onChange={(e) => {
            setSemester(e.target.value)
            setPage(0)
          }}
        />
      </div>

      <ErrorBanner error={query.error} />
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState icon="papers" title="No question papers match those filters" />
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Subject</th>
                  <th>Year</th>
                  <th>Type</th>
                  <th>Semester</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((qp) => (
                  <tr key={qp.questionPaperId}>
                    <td>
                      <Link to={`/materials/${qp.material.materialId}`}>{qp.material.title}</Link>
                    </td>
                    <td>{qp.material.subjectCode ?? '--'}</td>
                    <td>{qp.examYear ?? '--'}</td>
                    <td>{qp.examType ?? '--'}</td>
                    <td>{qp.semester ?? '--'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

function MembersTab({
  communityId,
  members,
  isOwner,
  loading,
}: {
  communityId: number
  members: { userId: number; username: string; displayName: string; memberRole: string; joinedAt: string }[] | undefined
  isOwner: boolean
  loading: boolean
}) {
  const queryClient = useQueryClient()
  const guestsQuery = useQuery({
    queryKey: ['community', communityId, 'guests'],
    queryFn: () => communitiesApi.guests(communityId),
    enabled: isOwner,
  })
  const removeGuestMutation = useMutation({
    mutationFn: (userId: number) => communitiesApi.removeGuest(communityId, userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['community', communityId, 'guests'] }),
  })

  return (
    <div className="stack">
      <h2 style={{ marginBottom: 0 }}>Members</h2>
      {loading && <Spinner />}
      {members && (
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
              {members.map((m) => (
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

      {isOwner && (
        <>
          <h2 style={{ marginBottom: 0, marginTop: 'var(--space-3)' }}>Share link guests</h2>
          <p className="text-sm muted" style={{ marginTop: 0 }}>
            People who opened this community through its share link without joining.
          </p>
          {guestsQuery.isLoading && <Spinner />}
          {guestsQuery.data && guestsQuery.data.length === 0 && (
            <EmptyState title="No guests yet" hint="Nobody has opened the share link without joining." />
          )}
          {guestsQuery.data && guestsQuery.data.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Access granted</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {guestsQuery.data.map((g) => (
                    <tr key={g.userId}>
                      <td>{g.username}</td>
                      <td className="text-sm muted">{formatDate(g.grantedAt)}</td>
                      <td>
                        <button
                          className="btn btn-sm btn-danger"
                          disabled={removeGuestMutation.isPending}
                          onClick={() => removeGuestMutation.mutate(g.userId)}
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <ErrorBanner error={removeGuestMutation.error} />
        </>
      )}
    </div>
  )
}

function ReportsTab() {
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['reports', page], queryFn: () => reportsApi.listPending(page) })

  const resolveMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: ReportStatus }) => reportsApi.resolve(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports'] }),
  })

  return (
    <div className="stack">
      <p className="text-sm muted" style={{ marginTop: 0 }}>
        Pending reports for communities you own or moderate (not limited to this one -- the reports endpoint doesn't
        yet support filtering by community).
      </p>

      <ErrorBanner error={query.error} />
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState icon="check" title="No pending reports" hint="Nothing needs your attention right now." />
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Material</th>
                  <th>Reported by</th>
                  <th>Reason</th>
                  <th>Date</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((r) => (
                  <tr key={r.reportId}>
                    <td>
                      <Link to={`/materials/${r.materialId}`}>{r.materialTitle}</Link>
                    </td>
                    <td>{r.reportedByUsername}</td>
                    <td>{r.reason}</td>
                    <td className="text-sm muted">{formatDate(r.createdAt)}</td>
                    <td>
                      <div className="row">
                        <button
                          className="btn btn-sm"
                          disabled={resolveMutation.isPending}
                          onClick={() => resolveMutation.mutate({ id: r.reportId, status: 'REVIEWED' })}
                        >
                          <Icon name="check" size={13} />
                          Mark reviewed
                        </button>
                        <button
                          className="btn btn-sm"
                          disabled={resolveMutation.isPending}
                          onClick={() => resolveMutation.mutate({ id: r.reportId, status: 'DISMISSED' })}
                        >
                          Dismiss
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
      <ErrorBanner error={resolveMutation.error} />
    </div>
  )
}
