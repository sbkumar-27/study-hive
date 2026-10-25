import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { materialsApi } from '../materials/materials'
import { communitiesApi } from '../communities/communities'
import { ErrorBanner } from '../../shared/components/ErrorBanner'
import { EmptyState } from '../../shared/components/EmptyState'
import { Icon } from '../../shared/components/Icon'
import { Pagination } from '../../shared/components/Pagination'
import { Spinner } from '../../shared/components/Spinner'
import { formatDate } from '../../shared/utils/formatDate'
import { formatBytes, fileExtensionOf } from '../../shared/utils/formatBytes'
import { materialTypeBadgeClass } from '../../shared/utils/materialType'
import type { Material } from '../../shared/types'

type Tab = 'overview' | 'uploads' | 'favorites' | 'communities' | 'vault'

const RECENT_UPLOADS_PAGE = 10

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? '' : ''
  return (first + last).toUpperCase() || '?'
}

export function ProfilePage() {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const initialTab = (searchParams.get('tab') as Tab | null) ?? 'overview'
  const [tab, setTab] = useState<Tab>(initialTab)
  const [vaultPage, setVaultPage] = useState(0)
  const [recentUploadsCount, setRecentUploadsCount] = useState(RECENT_UPLOADS_PAGE)

  // There's no dedicated "my favorites" endpoint today -- the closest existing one is the
  // general materials search, so this pulls a broad newest-first pool (everything the user can
  // currently see) and filters client-side to favorites. That means very old favorites outside
  // this pool, or favorited private/vault materials, may not show up here.
  const poolQuery = useQuery({
    queryKey: ['materials', 'profile-pool'],
    queryFn: () => materialsApi.search({ sort: 'newest', size: 200 }),
    enabled: !!user && tab === 'favorites',
  })
  const uploadsQuery = useQuery({
    queryKey: ['materials', 'mine', 50],
    queryFn: () => materialsApi.mine(0, 50),
    enabled: !!user && tab === 'uploads',
  })
  const recentUploadsQuery = useQuery({
    queryKey: ['materials', 'mine', recentUploadsCount],
    queryFn: () => materialsApi.mine(0, recentUploadsCount),
    enabled: !!user && tab === 'overview',
  })
  const communitiesQuery = useQuery({
    queryKey: ['communities', 'mine', 'profile'],
    queryFn: () => communitiesApi.mine(0, 100),
    enabled: !!user && (tab === 'communities' || tab === 'overview' || tab === 'uploads'),
  })
  const vaultQuery = useQuery({
    queryKey: ['vault', vaultPage],
    queryFn: () => materialsApi.vault(vaultPage),
    enabled: !!user && tab === 'vault',
  })

  if (!user) return null

  const myFavorites = (poolQuery.data?.content ?? []).filter((m) => m.favorited)
  const communityNameById = new Map<number, string>()
  for (const c of communitiesQuery.data?.content ?? []) {
    communityNameById.set(c.communityId, c.name)
  }
  const tabs: { key: Tab; label: string }[] = [
    { key: 'overview', label: 'Overview' },
    { key: 'uploads', label: 'Uploads' },
    { key: 'favorites', label: 'Favorites' },
    { key: 'communities', label: 'Communities' },
    { key: 'vault', label: 'My Vault' },
  ]

  return (
    <div className="stack">
      <div className="page-header">
        <h1>Profile</h1>
      </div>

      <div className="card row" style={{ maxWidth: 560 }}>
        <span className="avatar" style={{ width: 48, height: 48, fontSize: '1.1rem' }} aria-hidden="true">
          {initialsOf(user.displayName)}
        </span>
        <div>
          <h3 style={{ margin: 0 }}>{user.displayName}</h3>
          <p className="text-sm muted" style={{ margin: 0 }}>
            {user.username} &middot; {user.email}
          </p>
          <p className="text-sm muted" style={{ margin: 0 }}>
            Member since {formatDate(user.createdAt)}
          </p>
        </div>
      </div>

      <div className="tabs" role="tablist">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            className={`tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="stack">
          <div className="row-between">
            <h3 style={{ margin: 0 }}>Recent uploads</h3>
            <Link to="/materials/new" className="btn btn-primary btn-sm">
              <Icon name="upload" size={15} />
              Upload
            </Link>
          </div>
          {recentUploadsQuery.isLoading && <Spinner />}
          <ErrorBanner error={recentUploadsQuery.error} />
          {recentUploadsQuery.data && recentUploadsQuery.data.content.length === 0 && (
            <EmptyState
              icon="materials"
              title="You haven't uploaded anything yet"
              hint="Files you upload to a community or your vault will show up here."
              action={
                <Link to="/materials/new" className="btn btn-primary btn-sm">
                  Upload a file
                </Link>
              }
            />
          )}
          {recentUploadsQuery.data && recentUploadsQuery.data.content.length > 0 && (
            <>
              <UploadsList materials={recentUploadsQuery.data.content} communityNameById={communityNameById} />
              {recentUploadsQuery.data.content.length < recentUploadsQuery.data.totalElements && (
                <button
                  type="button"
                  className="btn"
                  onClick={() => setRecentUploadsCount((n) => n + RECENT_UPLOADS_PAGE)}
                  disabled={recentUploadsQuery.isFetching}
                  style={{ alignSelf: 'flex-start' }}
                >
                  {recentUploadsQuery.isFetching ? 'Loading...' : 'Show more'}
                </button>
              )}
            </>
          )}
        </div>
      )}

      {tab === 'uploads' && (
        <MaterialList
          loading={uploadsQuery.isLoading}
          error={uploadsQuery.error}
          materials={uploadsQuery.data?.content ?? []}
          emptyTitle="No uploads yet"
          emptyHint="Materials you upload to communities will show up here."
        />
      )}

      {tab === 'favorites' && (
        <MaterialList
          loading={poolQuery.isLoading}
          error={poolQuery.error}
          materials={myFavorites}
          emptyTitle="No favorites yet"
          emptyHint="Materials you favorite will show up here."
        />
      )}

      {tab === 'communities' && (
        <div className="stack">
          {communitiesQuery.isLoading && <Spinner />}
          <ErrorBanner error={communitiesQuery.error} />
          {communitiesQuery.data && communitiesQuery.data.content.length === 0 && (
            <EmptyState
              icon="communities"
              title="You haven't joined any communities yet"
              action={
                <Link to="/communities" className="btn btn-primary btn-sm">
                  Browse communities
                </Link>
              }
            />
          )}
          {communitiesQuery.data && communitiesQuery.data.content.length > 0 && (
            <div className="grid">
              {communitiesQuery.data.content.map((c) => (
                <Link key={c.communityId} to={`/communities/${c.communityId}`} className="card card-link">
                  <div className="row-between" style={{ gap: 8 }}>
                    <h3 style={{ margin: 0 }}>{c.name}</h3>
                    <span className="badge badge-primary">{c.currentUserRole}</span>
                  </div>
                  <p className="text-sm muted" style={{ marginBottom: 0 }}>
                    {c.memberCount} members
                  </p>
                </Link>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'vault' && (
        <div className="stack">
          <div className="row-between">
            <p className="text-sm muted" style={{ margin: 0 }}>
              <Icon name="lock" size={14} /> Private materials only you can see -- never shared with any community.
            </p>
            <Link to="/materials/new" className="btn btn-primary btn-sm">
              <Icon name="upload" size={15} />
              Upload to vault
            </Link>
          </div>

          <ErrorBanner error={vaultQuery.error} />
          {vaultQuery.isLoading && <Spinner />}

          {vaultQuery.data && vaultQuery.data.content.length === 0 && (
            <EmptyState
              icon="vault"
              title="Your vault is empty"
              hint="Upload a private note or exam prep file just for you."
              action={
                <Link to="/materials/new" className="btn btn-primary btn-sm">
                  Upload your first file
                </Link>
              }
            />
          )}

          {vaultQuery.data && vaultQuery.data.content.length > 0 && (
            <>
              <div className="grid">
                {vaultQuery.data.content.map((m) => (
                  <Link key={m.materialId} to={`/materials/${m.materialId}`} className="card card-link vault-card">
                    <div className="row" style={{ marginBottom: 8, alignItems: 'flex-start' }}>
                      <span className="card-icon">
                        <Icon name="lock" size={16} />
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <h3 style={{ margin: 0, fontSize: '1.02rem' }}>{m.title}</h3>
                        <span className="badge badge-vault" style={{ marginTop: 4 }}>
                          {m.materialType.replace('_', ' ')}
                        </span>
                      </div>
                    </div>
                    {m.description && <p className="text-sm">{m.description}</p>}
                    <p className="text-sm muted" style={{ marginBottom: 0 }}>
                      v{m.latestVersionNumber} &middot; updated {formatDate(m.updatedAt)}
                    </p>
                  </Link>
                ))}
              </div>
              <Pagination page={vaultQuery.data.page} totalPages={vaultQuery.data.totalPages} onChange={setVaultPage} />
            </>
          )}
        </div>
      )}
    </div>
  )
}

function UploadsList({
  materials,
  communityNameById,
}: {
  materials: Material[]
  communityNameById: Map<number, string>
}) {
  return (
    <div className="table-wrap">
      {materials.map((m) => {
        const fileName = m.latestVersionFileName ?? m.title
        const location =
          m.communityId === null ? 'My Vault' : (communityNameById.get(m.communityId) ?? 'a community')
        const subject = m.subjectCode ? ` · ${m.subjectCode}` : ''
        const ext = fileExtensionOf(m.latestVersionFileName)
        const size = m.latestVersionSizeBytes != null ? formatBytes(m.latestVersionSizeBytes) : null
        return (
          <Link key={m.materialId} to={`/materials/${m.materialId}?view=1`} className="upload-row">
            <Icon name={m.materialType === 'QUESTION_PAPER' ? 'papers' : 'file'} size={16} />
            <div className="upload-row-main">
              <span className="upload-row-title">{fileName}</span>
              <span className="text-sm muted">
                {location}
                {subject}
              </span>
            </div>
            <div className="upload-row-meta">
              <span className="text-sm muted">
                {ext || m.materialType.replace('_', ' ')}
                {size ? ` · ${size}` : ''}
              </span>
              <span className="text-sm muted">{formatDate(m.createdAt)}</span>
            </div>
          </Link>
        )
      })}
    </div>
  )
}

function MaterialList({
  loading,
  error,
  materials,
  emptyTitle,
  emptyHint,
}: {
  loading: boolean
  error: unknown
  materials: Material[]
  emptyTitle: string
  emptyHint: string
}) {
  return (
    <div className="stack">
      {loading && <Spinner />}
      <ErrorBanner error={error} />
      {!loading && materials.length === 0 && <EmptyState icon="materials" title={emptyTitle} hint={emptyHint} />}
      {materials.length > 0 && (
        <div className="table-wrap">
          {materials.map((m) => (
            <Link key={m.materialId} to={`/materials/${m.materialId}`} className="file-row">
              <Icon name={m.materialType === 'QUESTION_PAPER' ? 'papers' : 'file'} size={16} />
              <span className="file-row-title">{m.title}</span>
              <span className={materialTypeBadgeClass(m.materialType)}>{m.materialType.replace('_', ' ')}</span>
              {m.favorited && <Icon name="starFilled" size={14} style={{ color: 'var(--color-accent)' }} />}
              <span className="text-sm muted">{formatDate(m.createdAt)}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
