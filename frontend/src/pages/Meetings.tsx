import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createProjectMeeting, getProjectMeetings } from '../api/meetings'
import type { Meeting } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

export function Meetings() {
  const navigate = useNavigate()
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  // Add Meeting Form State
  const [showAddForm, setShowAddForm] = useState(false)
  const [title, setTitle] = useState('')
  const [meetingDate, setMeetingDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

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

  async function handleCreateMeeting(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)

    if (!title.trim()) {
      setFormError('Meeting title is required.')
      return
    }
    if (!meetingDate) {
      setFormError('Meeting date is required.')
      return
    }

    setSaving(true)
    try {
      const created = await createProjectMeeting(SELECTED_PROJECT_ID, {
        title: title.trim(),
        meetingDate,
        notes: notes.trim() || undefined,
      })
      setShowAddForm(false)
      setTitle('')
      setNotes('')
      // Navigate to the newly created meeting details page
      if (created.id != null) {
        navigate(`/meetings/${created.id}`)
      } else {
        const refreshed = await getProjectMeetings(SELECTED_PROJECT_ID)
        setMeetings(refreshed)
      }
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not create meeting')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          title="Meetings"
          description="Meeting notes and transcripts used for AI task and research extraction."
        />
        {!showAddForm ? (
          <button
            type="button"
            onClick={() => setShowAddForm(true)}
            className="self-start rounded-md bg-stone-950 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-stone-800 sm:self-center"
          >
            + Add Meeting
          </button>
        ) : null}
      </div>

      {showAddForm ? (
        <form
          onSubmit={(e) => void handleCreateMeeting(e)}
          className="mt-6 rounded-lg border border-stone-300 bg-white p-5 shadow-xs"
        >
          <div className="flex items-center justify-between border-b border-stone-200 pb-3">
            <h2 className="text-sm font-semibold tracking-tight text-stone-950">Add New Meeting</h2>
            <button
              type="button"
              onClick={() => {
                setShowAddForm(false)
                setFormError(null)
              }}
              className="text-xs text-stone-500 hover:text-stone-800"
            >
              Cancel
            </button>
          </div>

          {formError ? <Alert message={formError} /> : null}

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="meeting-title" className="block text-xs font-medium text-stone-700">
                Meeting Title <span className="text-red-500">*</span>
              </label>
              <input
                id="meeting-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Supervision Sync: ViT-B16 Pathology"
                className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                disabled={saving}
                required
              />
            </div>
            <div>
              <label htmlFor="meeting-date" className="block text-xs font-medium text-stone-700">
                Meeting Date <span className="text-red-500">*</span>
              </label>
              <input
                id="meeting-date"
                type="date"
                value={meetingDate}
                onChange={(e) => setMeetingDate(e.target.value)}
                className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                disabled={saving}
                required
              />
            </div>
          </div>

          <div className="mt-4">
            <label htmlFor="meeting-notes" className="block text-xs font-medium text-stone-700">
              Meeting Notes / Transcript
            </label>
            <textarea
              id="meeting-notes"
              rows={8}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Paste supervision notes, decisions, experiments, results, and action items..."
              className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 font-mono text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
              disabled={saving}
            />
            <p className="mt-1 text-xs text-stone-500">
              AI extraction parses notes for decisions, experiments, and actionable task assignments.
            </p>
          </div>

          <div className="mt-5 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={() => {
                setShowAddForm(false)
                setFormError(null)
              }}
              className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-md bg-stone-950 px-4 py-1.5 text-sm font-medium text-white hover:bg-stone-800 disabled:opacity-60"
            >
              {saving ? 'Saving Meeting…' : 'Save Meeting'}
            </button>
          </div>
        </form>
      ) : null}

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
