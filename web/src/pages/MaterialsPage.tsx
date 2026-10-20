import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { materialsApi } from '../api/materials'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import type { Material, MaterialType, SortOrder } from '../types'

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
      <div className="row-between">
        <h1>Materials</h1>
        <Link
          to={`/materials/new${communityId ? `?communityId=${communityId}` : ''}${
            subjectId ? `&subjectId=${subjectId}` : ''
          }`}
          className="btn btn-primary"
        >
          Upload material
        </Link>
      </div>

      <div className="filters">
        <input
          placeholder="Search title, book, author, subject..."
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
        />
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
        <EmptyState title="No materials found" hint="Try a different search or filter." />
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

function MaterialCard({ material }: { material: Material }) {
  return (
    <Link to={`/materials/${material.materialId}`} className="card card-link">
      <div className="row-between" style={{ marginBottom: 6 }}>
        <h3 style={{ margin: 0, fontSize: '1.05rem' }}>{material.title}</h3>
        {material.favorited && <span title="Favorited">&#9733;</span>}
      </div>
      <div className="row text-sm muted" style={{ marginBottom: 6 }}>
        <span className="badge">{material.materialType.replace('_', ' ')}</span>
        {material.subjectCode && <span>{material.subjectCode}</span>}
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
