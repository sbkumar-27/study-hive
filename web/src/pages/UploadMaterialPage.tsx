import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { materialsApi, type CreateMaterialRequest } from '../api/materials'
import { subjectsApi } from '../api/subjects'
import { ErrorBanner } from '../components/ErrorBanner'
import { Icon } from '../components/Icon'
import type { MaterialType } from '../types'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const TYPES: MaterialType[] = ['NOTE', 'TEXTBOOK', 'QUESTION_PAPER', 'PRESENTATION', 'OTHER']

export function UploadMaterialPage() {
  const [searchParams] = useSearchParams()
  const communityId = searchParams.get('communityId') ? Number(searchParams.get('communityId')) : undefined
  const initialSubjectId = searchParams.get('subjectId') ? Number(searchParams.get('subjectId')) : undefined
  const navigate = useNavigate()

  const subjectsQuery = useQuery({
    queryKey: ['community', communityId, 'subjects'],
    queryFn: () => subjectsApi.list(communityId as number),
    enabled: communityId !== undefined,
  })

  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [materialType, setMaterialType] = useState<MaterialType>('NOTE')
  const [subjectId, setSubjectId] = useState<number | ''>(initialSubjectId ?? '')
  const [bookName, setBookName] = useState('')
  const [authorName, setAuthorName] = useState('')
  const [examYear, setExamYear] = useState('')
  const [examType, setExamType] = useState('')
  const [semester, setSemester] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file) {
      setError(new Error('Please choose a file to upload'))
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      const metadata: CreateMaterialRequest = {
        title,
        description: description || undefined,
        materialType,
        communityId,
        subjectId: communityId && subjectId !== '' ? Number(subjectId) : undefined,
        bookName: bookName || undefined,
        authorName: authorName || undefined,
      }
      if (materialType === 'QUESTION_PAPER') {
        metadata.examYear = examYear ? Number(examYear) : undefined
        metadata.examType = examType || undefined
        metadata.semester = semester ? Number(semester) : undefined
      }
      const material = await materialsApi.upload(file, metadata)
      navigate(`/materials/${material.materialId}`)
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="stack">
      <div>
        <h1 style={{ marginBottom: 4 }}>{communityId ? 'Upload material' : 'Upload to your Personal Vault'}</h1>
        {!communityId && (
          <p className="page-subtitle" style={{ marginBottom: 0 }}>
            <Icon name="lock" size={14} /> No community selected, so this upload will go to your private Personal
            Vault -- visible only to you.
          </p>
        )}
      </div>
      <form className="form form-wide card" onSubmit={handleSubmit}>
        <ErrorBanner error={error} />

        <div className="form-group">
          <label htmlFor="file">File</label>
          <label className="dropzone" htmlFor="file">
            <Icon name="upload" size={28} />
            {file ? (
              <>
                <span className="dropzone-filename">{file.name}</span>
                <span className="text-sm">{formatBytes(file.size)} -- click to change</span>
              </>
            ) : (
              <>
                <span className="dropzone-filename">Click to choose a file</span>
                <span className="text-sm">or drag it here</span>
              </>
            )}
            <input
              id="file"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
            />
          </label>
        </div>

        <div className="form-group">
          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required autoFocus />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description (optional)</label>
          <textarea id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>

        <div className="form-group">
          <label htmlFor="materialType">Type</label>
          <select id="materialType" value={materialType} onChange={(e) => setMaterialType(e.target.value as MaterialType)}>
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {t.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>

        {communityId && subjectsQuery.data && subjectsQuery.data.length > 0 && (
          <div className="form-group">
            <label htmlFor="subjectId">Subject (optional)</label>
            <select id="subjectId" value={subjectId} onChange={(e) => setSubjectId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">No specific subject</option>
              {subjectsQuery.data.map((s) => (
                <option key={s.subjectId} value={s.subjectId}>
                  {s.subjectCode} -- {s.subjectName}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="form-group">
          <label htmlFor="bookName">Book name (optional)</label>
          <input id="bookName" value={bookName} onChange={(e) => setBookName(e.target.value)} />
        </div>
        <div className="form-group">
          <label htmlFor="authorName">Author (optional)</label>
          <input id="authorName" value={authorName} onChange={(e) => setAuthorName(e.target.value)} />
        </div>

        {materialType === 'QUESTION_PAPER' && (
          <div className="row">
            <div className="form-group">
              <label htmlFor="examYear">Exam year</label>
              <input
                id="examYear"
                type="number"
                min={1900}
                max={2100}
                value={examYear}
                onChange={(e) => setExamYear(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label htmlFor="examType">Exam type</label>
              <input id="examType" placeholder="END_SEM, MID_SEM..." value={examType} onChange={(e) => setExamType(e.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="semester">Semester</label>
              <input
                id="semester"
                type="number"
                min={1}
                max={12}
                value={semester}
                onChange={(e) => setSemester(e.target.value)}
              />
            </div>
          </div>
        )}

        {submitting && (
          <div className="stack" style={{ gap: 4 }}>
            <div className="progress-bar">
              <div className="progress-bar-fill" />
            </div>
            <span className="text-sm muted">Uploading your file, please wait...</span>
          </div>
        )}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? (
            'Uploading...'
          ) : (
            <>
              <Icon name="upload" size={16} />
              Upload
            </>
          )}
        </button>
      </form>
    </div>
  )
}
