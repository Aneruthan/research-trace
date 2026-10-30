import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjectMeetings } from '../api/meetings'
import type { Meeting } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

export function Meetings() {
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)

    getProjectMeetings(SELECTED_PROJECT_ID, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setMeetings(data)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setError(cause instanceof Error ? cause.message : 'Could not load meetings')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [])

  return (
    <section>
      <PageHeader
        title="Meetings"
        description="Meeting notes and transcripts used for AI task extraction."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading meetings for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && error ? <Alert message={error} /> : null}
      {!loading && !error && meetings.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No meetings for this project.</p>
      ) : null}
      {!loading && !error && meetings.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {meetings.map((meeting) => (
            <li key={meeting.id ?? meeting.title}>
              <Link
                to={`/meetings/${meeting.id}`}
                className="flex flex-col gap-1 px-4 py-4 transition-colors hover:bg-stone-50 sm:flex-row sm:items-center sm:justify-between"
              >
                <span className="text-sm font-medium text-stone-950">{meeting.title ?? 'Untitled meeting'}</span>
                <span className="text-xs text-stone-500">
                  {meeting.meetingDate ?? 'No date'}
                  {' · '}
                  {hasNotes(meeting) ? 'Notes available' : 'No notes'}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}

function hasNotes(meeting: Meeting): boolean {
  return meeting.notes != null && meeting.notes.trim().length > 0
}

function Alert({ message }: { message: string }) {
  return (
    <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      {message}
    </p>
  )
}
