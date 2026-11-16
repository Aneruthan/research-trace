const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/

/** Today's date (or the given date) as YYYY-MM-DD in the browser's time zone. */
export function localIsoDate(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Formats a backend date or timestamp. Date-only values (LocalDate) are returned unchanged:
 * `new Date('YYYY-MM-DD')` parses them as UTC midnight, which shifts the day and invents a time.
 */
export function formatDateOrTimestamp(value: string | null): string | null {
  if (value == null || value.trim() === '') {
    return null
  }
  if (DATE_ONLY.test(value.trim())) {
    return value.trim()
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return parsed.toLocaleString()
}
