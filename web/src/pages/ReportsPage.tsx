import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { reportsApi } from '../api/reports'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Icon } from '../components/Icon'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import type { ReportStatus } from '../types'
import { formatDate } from '../utils/formatDate'

export function ReportsPage() {
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['reports', page], queryFn: () => reportsApi.listPending(page) })

  const resolveMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: ReportStatus }) => reportsApi.resolve(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports'] }),
  })

  return (
    <div className="stack">
      <div className="page-header" style={{ marginBottom: 0 }}>
        <div>
          <h1>Pending reports</h1>
          <p className="page-subtitle">
            Shows all pending reports if you're an admin, or reports for communities you own/moderate otherwise.
          </p>
        </div>
      </div>

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
