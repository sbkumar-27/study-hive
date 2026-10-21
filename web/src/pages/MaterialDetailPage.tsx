import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { materialsApi } from '../api/materials'
import { reportsApi } from '../api/reports'
import { DocumentViewer } from '../components/DocumentViewer'
import { ErrorBanner } from '../components/ErrorBanner'
import { Icon } from '../components/Icon'
import { Spinner } from '../components/Spinner'
import { useAuth } from '../context/AuthContext'
import { materialTypeBadgeClass } from '../utils/materialType'
import { fileTooLargeMessage } from '../utils/upload'
import type { MaterialVersion } from '../types'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function MaterialDetailPage() {
  const { id } = useParams<{ id: string }>()
  const materialId = Number(id)
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const viewRecorded = useRef(false)

  const materialQuery = useQuery({
    queryKey: ['material', materialId],
    queryFn: () => materialsApi.get(materialId),
  })
  const versionsQuery = useQuery({
    queryKey: ['material', materialId, 'versions'],
    queryFn: () => materialsApi.versions(materialId),
    enabled: !materialQuery.isError,
  })

  useEffect(() => {
    if (materialQuery.data && !viewRecorded.current) {
      viewRecorded.current = true
      materialsApi
        .recordView(materialId)
        .then(() => queryClient.invalidateQueries({ queryKey: ['material', materialId] }))
        .catch(() => {
          // Non-critical; ignore failures recording a view.
        })
    }
  }, [materialQuery.data, materialId, queryClient])

  const favoriteMutation = useMutation({
    mutationFn: () =>
      materialQuery.data?.favorited ? materialsApi.unfavorite(materialId) : materialsApi.favorite(materialId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['material', materialId] }),
  })

  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({ title: '', description: '', bookName: '', authorName: '' })
  const updateMutation = useMutation({
    mutationFn: () => materialsApi.update(materialId, editForm),
    onSuccess: () => {
      setEditing(false)
      queryClient.invalidateQueries({ queryKey: ['material', materialId] })
    },
  })

  const [newVersionFile, setNewVersionFile] = useState<File | null>(null)
  const [versionNotes, setVersionNotes] = useState('')
  const [showVersionForm, setShowVersionForm] = useState(false)
  const addVersionMutation = useMutation({
    mutationFn: () => {
      if (!newVersionFile) throw new Error('Choose a file first')
      const tooLarge = fileTooLargeMessage(newVersionFile)
      if (tooLarge) throw new Error(tooLarge)
      return materialsApi.addVersion(materialId, newVersionFile, versionNotes || undefined)
    },
    onSuccess: () => {
      setNewVersionFile(null)
      setVersionNotes('')
      setShowVersionForm(false)
      queryClient.invalidateQueries({ queryKey: ['material', materialId] })
      queryClient.invalidateQueries({ queryKey: ['material', materialId, 'versions'] })
    },
  })

  const [reportReason, setReportReason] = useState('')
  const [showReportForm, setShowReportForm] = useState(false)
  const reportMutation = useMutation({
    mutationFn: () => reportsApi.create({ materialId, reason: reportReason }),
    onSuccess: () => {
      setReportReason('')
      setShowReportForm(false)
    },
  })

  const [downloadError, setDownloadError] = useState<unknown>(null)
  const [previewing, setPreviewing] = useState<MaterialVersion | null>(null)

  if (materialQuery.isLoading) return <Spinner />
  if (materialQuery.error) return <ErrorBanner error={materialQuery.error} />
  const material = materialQuery.data
  if (!material) return null

  const isOwner = material.uploadedByUserId === user?.userId

  const startEditing = () => {
    setEditForm({
      title: material.title,
      description: material.description ?? '',
      bookName: material.bookName ?? '',
      authorName: material.authorName ?? '',
    })
    setEditing(true)
  }

  const handleDownloadLatest = async () => {
    setDownloadError(null)
    try {
      await materialsApi.downloadLatest(materialId, material.title)
    } catch (err) {
      setDownloadError(err)
    }
  }

  async function handleDownloadVersion(versionId: number, filename: string) {
    setDownloadError(null)
    try {
      await materialsApi.downloadVersion(materialId, versionId, filename)
    } catch (err) {
      setDownloadError(err)
    }
  }

  return (
    <div className="stack">
      {!editing ? (
        <>
          <div className="page-header" style={{ marginBottom: 0 }}>
            <h1 style={{ marginBottom: 4 }}>{material.title}</h1>
            <div className="actions">
              <button
                className="btn"
                onClick={() => favoriteMutation.mutate()}
                disabled={favoriteMutation.isPending}
              >
                <Icon
                  name={material.favorited ? 'starFilled' : 'star'}
                  size={16}
                  style={{ color: material.favorited ? '#f0a500' : undefined }}
                />
                {material.favorited ? 'Favorited' : 'Favorite'}
              </button>
              <button className="btn btn-primary" onClick={handleDownloadLatest}>
                <Icon name="download" size={16} />
                Download latest
              </button>
            </div>
          </div>

          <div className="row text-sm muted">
            <span className={materialTypeBadgeClass(material.materialType)}>
              {material.materialType.replace('_', ' ')}
            </span>
            {material.visibility === 'PRIVATE' && (
              <span className="badge badge-vault">
                <Icon name="lock" size={11} />
                Vault
              </span>
            )}
            {material.subjectCode && <span>{material.subjectCode} -- {material.subjectName}</span>}
            <span>&middot;</span>
            <span>{material.viewCount} views</span>
            <span>&middot;</span>
            <span>uploaded by {material.uploadedByUsername}</span>
          </div>

          {material.description && <p>{material.description}</p>}
          {(material.bookName || material.authorName) && (
            <p className="text-sm muted">
              {material.bookName && <>Book: {material.bookName}&nbsp;&nbsp;</>}
              {material.authorName && <>Author: {material.authorName}</>}
            </p>
          )}

          <ErrorBanner error={downloadError} />

          <div className="row">
            {isOwner && (
              <button className="btn" onClick={startEditing}>
                Edit details
              </button>
            )}
            {!isOwner && (
              <button className="btn" onClick={() => setShowReportForm((v) => !v)}>
                <Icon name="warning" size={15} />
                Report this material
              </button>
            )}
          </div>

          {showReportForm && (
            <form
              className="card row"
              onSubmit={(e: FormEvent) => {
                e.preventDefault()
                reportMutation.mutate()
              }}
            >
              <input
                placeholder="Reason for reporting"
                value={reportReason}
                onChange={(e) => setReportReason(e.target.value)}
                required
                style={{ flex: 1 }}
              />
              <button className="btn btn-danger" type="submit" disabled={reportMutation.isPending}>
                Submit report
              </button>
            </form>
          )}
          {reportMutation.isSuccess && <div className="alert alert-success">Report submitted. A moderator will review it.</div>}
          <ErrorBanner error={reportMutation.error} />
        </>
      ) : (
        <form
          className="form form-wide card"
          onSubmit={(e: FormEvent) => {
            e.preventDefault()
            updateMutation.mutate()
          }}
        >
          <h2>Edit material</h2>
          <ErrorBanner error={updateMutation.error} />
          <div className="form-group">
            <label htmlFor="edit-title">Title</label>
            <input
              id="edit-title"
              value={editForm.title}
              onChange={(e) => setEditForm((f) => ({ ...f, title: e.target.value }))}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="edit-description">Description</label>
            <textarea
              id="edit-description"
              value={editForm.description}
              onChange={(e) => setEditForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <div className="form-group">
            <label htmlFor="edit-book">Book name</label>
            <input
              id="edit-book"
              value={editForm.bookName}
              onChange={(e) => setEditForm((f) => ({ ...f, bookName: e.target.value }))}
            />
          </div>
          <div className="form-group">
            <label htmlFor="edit-author">Author</label>
            <input
              id="edit-author"
              value={editForm.authorName}
              onChange={(e) => setEditForm((f) => ({ ...f, authorName: e.target.value }))}
            />
          </div>
          <div className="row">
            <button type="submit" className="btn btn-primary" disabled={updateMutation.isPending}>
              Save changes
            </button>
            <button type="button" className="btn" onClick={() => setEditing(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <hr className="divider" />

      <div className="row-between">
        <h2 style={{ marginBottom: 0 }}>Version history</h2>
        {isOwner && (
          <button className="btn btn-sm" onClick={() => setShowVersionForm((v) => !v)}>
            {showVersionForm ? (
              'Cancel'
            ) : (
              <>
                <Icon name="plus" size={14} />
                Upload new version
              </>
            )}
          </button>
        )}
      </div>

      {showVersionForm && (
        <form
          className="card stack"
          onSubmit={(e: FormEvent) => {
            e.preventDefault()
            addVersionMutation.mutate()
          }}
        >
          <ErrorBanner error={addVersionMutation.error} />
          <div className="form-group">
            <label htmlFor="version-file">File</label>
            <input
              id="version-file"
              type="file"
              onChange={(e) => setNewVersionFile(e.target.files?.[0] ?? null)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="version-notes">What changed? (optional)</label>
            <input
              id="version-notes"
              value={versionNotes}
              onChange={(e) => setVersionNotes(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={addVersionMutation.isPending}>
            {addVersionMutation.isPending ? 'Uploading...' : 'Upload version'}
          </button>
          <p className="hint">
            This adds a new version without deleting any previous ones -- old versions stay downloadable below.
          </p>
        </form>
      )}

      {versionsQuery.isLoading && <Spinner />}
      {versionsQuery.data && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Version</th>
                <th>File</th>
                <th>Size</th>
                <th>Uploaded by</th>
                <th>Date</th>
                <th>Notes</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {versionsQuery.data.map((v, i) => (
                <tr key={v.versionId}>
                  <td>
                    v{v.versionNumber}
                    {i === 0 && (
                      <span className="badge badge-primary" style={{ marginLeft: 6 }}>
                        Latest
                      </span>
                    )}
                  </td>
                  <td>{v.originalFileName}</td>
                  <td>{formatBytes(v.fileSizeBytes)}</td>
                  <td>{v.uploadedByUsername}</td>
                  <td className="text-sm muted">{new Date(v.createdAt).toLocaleString()}</td>
                  <td className="text-sm muted">{v.versionNotes ?? '--'}</td>
                  <td>
                    <div className="row" style={{ gap: 6, flexWrap: 'nowrap' }}>
                      <button className="btn btn-sm" onClick={() => setPreviewing(v)}>
                        <Icon name="file" size={13} />
                        View
                      </button>
                      <button
                        className="btn btn-sm"
                        onClick={() => handleDownloadVersion(v.versionId, v.originalFileName)}
                      >
                        <Icon name="download" size={13} />
                        Download
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {previewing && (
        <DocumentViewer
          materialId={materialId}
          versionId={previewing.versionId}
          fileName={previewing.originalFileName}
          mimeType={previewing.mimeType}
          onClose={() => setPreviewing(null)}
          onDownload={() => handleDownloadVersion(previewing.versionId, previewing.originalFileName)}
        />
      )}
    </div>
  )
}
