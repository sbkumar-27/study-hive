interface PaginationProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        className="btn btn-sm"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
        aria-label="Previous page"
      >
        &larr; Previous
      </button>
      <span className="text-sm muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn btn-sm"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
        aria-label="Next page"
      >
        Next &rarr;
      </button>
    </nav>
  )
}
