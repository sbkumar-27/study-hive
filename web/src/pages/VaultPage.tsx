import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { materialsApi } from '../api/materials'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'

export function VaultPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({ queryKey: ['vault', page], queryFn: () => materialsApi.vault(page) })

  return (
    <div className="stack">
      <div className="row-between">
        <div>
          <h1 style={{ marginBottom: 4 }}>My Personal Vault</h1>
          <p className="text-sm muted" style={{ marginBottom: 0 }}>
            Private materials only you can see.
          </p>
        </div>
        <Link to="/materials/new" className="btn btn-primary">
          Upload to vault
        </Link>
      </div>

      <ErrorBanner error={query.error} />
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <EmptyState title="Your vault is empty" hint="Upload a private note or exam prep file just for you." />
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="grid">
            {query.data.content.map((m) => (
              <Link key={m.materialId} to={`/materials/${m.materialId}`} className="card card-link">
                <h3 style={{ marginBottom: 6, fontSize: '1.05rem' }}>{m.title}</h3>
                <span className="badge">{m.materialType.replace('_', ' ')}</span>
                {m.description && <p className="text-sm" style={{ marginTop: 8 }}>{m.description}</p>}
              </Link>
            ))}
          </div>
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
