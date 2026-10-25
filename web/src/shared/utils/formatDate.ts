// Every date/time in the UI renders in Indian format and IST, regardless of the viewer's own
// browser locale/timezone -- the backend always sends UTC ISO strings; only display formatting
// happens here, nothing about how dates are stored or sent changes.
const PART_FORMATTER = new Intl.DateTimeFormat('en-IN', {
  timeZone: 'Asia/Kolkata',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: true,
})

function partsOf(value: string | number | Date): Record<string, string> {
  const date = value instanceof Date ? value : new Date(value)
  const parts: Record<string, string> = {}
  for (const { type, value: partValue } of PART_FORMATTER.formatToParts(date)) {
    parts[type] = partValue
  }
  return parts
}

/** DD/MM/YYYY, in IST -- e.g. 21/10/2026. */
export function formatDate(value: string | number | Date): string {
  const p = partsOf(value)
  return `${p.day}/${p.month}/${p.year}`
}

/** DD/MM/YYYY, hh:mm AM/PM, in IST -- e.g. 21/10/2026, 06:24 PM. */
export function formatDateTime(value: string | number | Date): string {
  const p = partsOf(value)
  const hour = p.hour.padStart(2, '0')
  const period = (p.dayPeriod ?? '').toUpperCase()
  return `${p.day}/${p.month}/${p.year}, ${hour}:${p.minute} ${period}`
}
