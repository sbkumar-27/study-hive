import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { materialsApi } from '../api/materials'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Icon } from '../components/Icon'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import type { Material, MaterialType, SortOrder } from '../types'
import { materialTypeBadgeClass } from '../utils/materialType'

const TYPES: MaterialType[] = ['NOTE', 'TEXTBOOK', 'QUESTION_PAPER', 'PRESENTATION', 'OTHER']

export function MaterialsPage() {
  const [searchParams] = useSearchParams()
  const communityId = searchParams.get('communityId') ? Number(searchParams.get('communityId')) : undefined
  const subjectId = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : undefined

  const [q, setQ] = useState('')
  const [type, setType] = useState<MaterialType | ''>('')
  const [sort, setSort] = useState<SortOrder>('newest')
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['materials', communityId, subjectId, q, type, sort, page],
    queryFn: () =>
      materialsApi.search({
        q: q || undefined,
        communityId,
        subjectId,
        type: type || undefined,
        sort,
        page,
      }),
  })

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Materials</h1>
          <p className="page-subtitle">Notes, textbooks, slides, and papers shared across your communities.</p>
        </div>
        <div className="actions">
          <Link
            to={`/materials/new${communityId ? `?communityId=${communityId}` : ''}${
              subjectId ? `&subjectId=${subjectId}` : ''
            }`}
            className="btn btn-primary"
          >
            <Icon name="upload" size={16} />
            Upload material
          </Link>
        </div>
      </div>

      <div className="filters">
        <div style={{ position: 'relative', flex: '1 1 240px' }}>
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
            placeholder="Search title, book, author, subject..."
            value={q}
            onChange={(e) => {
              setQ(e.target.value)
              setPage(0)
            }}
            style={{ paddingLeft: 34, width: '100%' }}
          />
        </div>
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value as MaterialType | '')
            setPage(0)
          }}
        >
          <option value="">All types</option>
          {TYPES.map((t) => (
            <option key={t} value={t}>
              {t.replace('_', ' ')}
            </option>
          ))}
        </select>
        <select
          value={sort}
          onChange={(e) => {
            setSort(e.target.value as SortOrder)
            setPage(0)
          }}
        >
          <option value="newest">Newest first</option>
          <option value="popular">Most popular</option>
        </select>
      </div>

      <ErrorBanner error={query.error} />
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState icon="materials" title="No materials found" hint="Try a different search or filter." />
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="grid">
            {query.data.content.map((m) => (
              <MaterialCard key={m.materialId} material={m} />
            ))}
          </div>
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function MaterialTypeIcon({ type }: { type: MaterialType }) {
  const icon = type === 'QUESTION_PAPER' ? 'papers' : 'file'
  return (
    <span className="card-icon">
      <Icon name={icon} size={18} />
    </span>
  )
}

function MaterialCard({ material }: { material: Material }) {
  return (
    <Link to={`/materials/${material.materialId}`} className="card card-link">
      <div className="row" style={{ marginBottom: 10, alignItems: 'flex-start' }}>
        <MaterialTypeIcon type={material.materialType} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="row-between" style={{ gap: 8 }}>
            <h3 style={{ margin: 0, fontSize: '1.02rem' }}>{material.title}</h3>
            {material.favorited && (
              <span title="Favorited" style={{ color: '#f0a500' }}>
                <Icon name="starFilled" size={16} />
              </span>
            )}
          </div>
          <div className="row text-sm muted" style={{ marginTop: 4 }}>
            <span className={materialTypeBadgeClass(material.materialType)}>
              {material.materialType.replace('_', ' ')}
            </span>
            {material.subjectCode && <span>{material.subjectCode}</span>}
          </div>
        </div>
      </div>
      {material.description && <p className="text-sm">{material.description}</p>}
      <div className="row text-sm muted">
        <span>v{material.latestVersionNumber}</span>
        <span>&middot;</span>
        <span>{material.viewCount} views</span>
        <span>&middot;</span>
        <span>by {material.uploadedByUsername}</span>
      </div>
    </Link>
  )
}
