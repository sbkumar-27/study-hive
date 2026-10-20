import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { questionPapersApi } from '../api/questionPapers'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'

export function QuestionPapersPage() {
  const [searchParams] = useSearchParams()
  const initialSubjectId = searchParams.get('subjectId') ?? ''

  const [subjectId, setSubjectId] = useState(initialSubjectId)
  const [examYear, setExamYear] = useState('')
  const [examType, setExamType] = useState('')
  const [semester, setSemester] = useState('')
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['question-papers', subjectId, examYear, examType, semester, page],
    queryFn: () =>
      questionPapersApi.search({
        subjectId: subjectId ? Number(subjectId) : undefined,
        examYear: examYear ? Number(examYear) : undefined,
        examType: examType || undefined,
        semester: semester ? Number(semester) : undefined,
        page,
      }),
  })

  return (
    <div className="stack">
      <div className="page-header" style={{ marginBottom: 0 }}>
        <div>
          <h1>Question papers</h1>
          <p className="page-subtitle">
            Filter by subject ID (find it on a community's subject card URL), exam year, exam type, or semester.
          </p>
        </div>
      </div>

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
