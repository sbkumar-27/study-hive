import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { materialsApi } from '../api/materials'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Icon } from '../components/Icon'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import { formatDate } from '../utils/formatDate'

export function VaultPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({ queryKey: ['vault', page], queryFn: () => materialsApi.vault(page) })

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>My Personal Vault</h1>
          <p className="page-subtitle">
            <Icon name="lock" size={14} /> Private materials only you can see -- never shared with any community.
          </p>
        </div>
        <div className="actions">
          <Link to="/materials/new" className="btn btn-primary">
            <Icon name="upload" size={16} />
            Upload to vault
          </Link>
        </div>
      </div>

      <ErrorBanner error={query.error} />
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
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

      {query.data && query.data.content.length > 0 && (
        <>
          <div className="grid">
            {query.data.content.map((m) => (
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
          <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
