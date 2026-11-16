import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  createProjectMeeting,
  deleteMeeting,
  getProjectMeetings,
  updateMeeting,
} from '../api/meetings'
import type { Meeting } from '../api/types'
import { localIsoDate } from '../components/common/dates'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

export function Meetings() {
  const navigate = useNavigate()
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  // Add Meeting Form State
  const [showAddForm, setShowAddForm] = useState(false)
  const [title, setTitle] = useState('')
  const [meetingDate, setMeetingDate] = useState(() => localIsoDate())
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Edit Meeting State
  const [editingMeetingId, setEditingMeetingId] = useState<number | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editDate, setEditDate] = useState('')
  const [editNotes, setEditNotes] = useState('')
  const [savingEdit, setSavingEdit] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)

  // Delete Meeting State
  const [meetingToDelete, setMeetingToDelete] = useState<Meeting | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  function loadMeetings(signal?: AbortSignal) {
    setLoading(true)
    setError(null)
    getProjectMeetings(SELECTED_PROJECT_ID, signal)
      .then((data) => {
        if (!signal?.aborted) {
          setMeetings(data)
        }
      })
      .catch((cause: unknown) => {
        if (signal?.aborted) return
        setError(cause instanceof Error ? cause.message : 'Could not load meetings')
      })
      .finally(() => {
        if (!signal?.aborted) {
          setLoading(false)
        }
      })
  }

  useEffect(() => {
    const controller = new AbortController()
    loadMeetings(controller.signal)
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
      setSuccessMessage(`Meeting "${created.title}" created successfully.`)
      if (created.id != null) {
        navigate(`/meetings/${created.id}`)
      } else {
        loadMeetings()
      }
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not create meeting')
    } finally {
      setSaving(false)
    }
  }

  function startEdit(meeting: Meeting) {
    if (meeting.id == null) return
    setEditingMeetingId(meeting.id)
    setEditTitle(meeting.title ?? '')
    setEditDate(meeting.meetingDate ?? '')
    setEditNotes(meeting.notes ?? '')
    setEditError(null)
    setSuccessMessage(null)
  }

  function cancelEdit() {
    setEditingMeetingId(null)
    setEditError(null)
  }

  async function handleSaveEdit(e: React.FormEvent) {
    e.preventDefault()
    if (editingMeetingId == null) return
    if (!editTitle.trim()) {
      setEditError('Meeting title cannot be blank.')
      return
    }
    if (!editDate) {
      setEditError('Meeting date is required.')
      return
    }

    setSavingEdit(true)
    setEditError(null)
    try {
      const updated = await updateMeeting(editingMeetingId, {
        title: editTitle.trim(),
        meetingDate: editDate,
        notes: editNotes.trim() || undefined,
      })
      setMeetings((prev) =>
        prev.map((m) => (m.id === editingMeetingId ? { ...m, ...updated } : m)),
      )
      setEditingMeetingId(null)
      setSuccessMessage(`Meeting "${updated.title}" updated successfully.`)
    } catch (cause: unknown) {
      setEditError(cause instanceof Error ? cause.message : 'Could not update meeting')
    } finally {
      setSavingEdit(false)
    }
  }

  function confirmDelete(meeting: Meeting) {
    setMeetingToDelete(meeting)
    setDeleteError(null)
    setSuccessMessage(null)
  }

  function cancelDelete() {
    setMeetingToDelete(null)
    setDeleteError(null)
  }

  async function handleDeleteMeeting() {
    if (!meetingToDelete || meetingToDelete.id == null) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteMeeting(meetingToDelete.id)
      setMeetings((prev) => prev.filter((m) => m.id !== meetingToDelete.id))
      setSuccessMessage(`Meeting "${meetingToDelete.title}" deleted successfully.`)
      setMeetingToDelete(null)
    } catch (cause: unknown) {
      setDeleteError(cause instanceof Error ? cause.message : 'Could not delete meeting')
    } finally {
      setDeleting(false)
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
            onClick={() => {
              setShowAddForm(true)
              setSuccessMessage(null)
              setError(null)
            }}
            className="self-start rounded-md bg-stone-950 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-stone-800 sm:self-center"
          >
            + Add Meeting
          </button>
        ) : null}
      </div>

      {successMessage ? (
        <div className="mt-4 rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800 flex items-center justify-between">
          <span>{successMessage}</span>
          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="text-xs text-green-700 hover:text-green-900 font-medium"
          >
            Dismiss
          </button>
        </div>
      ) : null}

      {error ? <Alert message={error} /> : null}

      {/* Delete Confirmation Modal / Banner */}
      {meetingToDelete ? (
        <div className="mt-6 rounded-lg border border-red-300 bg-red-50 p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-red-900">Confirm Meeting Deletion</h3>
          <p className="mt-2 text-sm text-red-800">
            Are you sure you want to delete meeting{' '}
            <strong className="font-semibold text-red-950">"{meetingToDelete.title}"</strong> (ID: {meetingToDelete.id})?
            This will permanently remove the meeting.
          </p>
          {deleteError ? (
            <div className="mt-3 rounded border border-red-400 bg-white p-3 text-xs text-red-700">
              <strong>Deletion Rejected:</strong> {deleteError}
            </div>
          ) : null}
          <div className="mt-4 flex items-center gap-3">
            <button
              type="button"
              onClick={() => void handleDeleteMeeting()}
              disabled={deleting}
              className="rounded-md bg-red-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-xs hover:bg-red-500 disabled:opacity-50"
            >
              {deleting ? 'Deleting…' : 'Yes, Delete Meeting'}
            </button>
            <button
              type="button"
              onClick={cancelDelete}
              disabled={deleting}
              className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-xs font-medium text-stone-700 hover:bg-stone-50"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : null}

      {/* Add Meeting Form */}
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

      {/* Edit Meeting Form */}
      {editingMeetingId != null ? (
        <form
          onSubmit={(e) => void handleSaveEdit(e)}
          className="mt-6 rounded-lg border border-amber-300 bg-amber-50/30 p-5 shadow-xs"
        >
          <div className="flex items-center justify-between border-b border-stone-200 pb-3">
            <h2 className="text-sm font-semibold tracking-tight text-stone-950">
              Edit Meeting (ID: {editingMeetingId})
            </h2>
            <button
              type="button"
              onClick={cancelEdit}
              className="text-xs text-stone-500 hover:text-stone-800"
            >
              Cancel
            </button>
          </div>

          {editError ? <Alert message={editError} /> : null}

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="edit-meeting-title" className="block text-xs font-medium text-stone-700">
                Meeting Title <span className="text-red-500">*</span>
              </label>
              <input
                id="edit-meeting-title"
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                disabled={savingEdit}
                required
              />
            </div>
            <div>
              <label htmlFor="edit-meeting-date" className="block text-xs font-medium text-stone-700">
                Meeting Date <span className="text-red-500">*</span>
              </label>
              <input
                id="edit-meeting-date"
                type="date"
                value={editDate}
                onChange={(e) => setEditDate(e.target.value)}
                className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                disabled={savingEdit}
                required
              />
            </div>
          </div>

          <div className="mt-4">
            <label htmlFor="edit-meeting-notes" className="block text-xs font-medium text-stone-700">
              Meeting Notes / Transcript
            </label>
            <textarea
              id="edit-meeting-notes"
              rows={10}
              value={editNotes}
              onChange={(e) => setEditNotes(e.target.value)}
              placeholder="Paste supervision notes, decisions, experiments, results, and action items..."
              className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 font-mono text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
              disabled={savingEdit}
            />
          </div>

          <div className="mt-5 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={cancelEdit}
              className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
              disabled={savingEdit}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={savingEdit}
              className="rounded-md bg-stone-950 px-4 py-1.5 text-sm font-medium text-white hover:bg-stone-800 disabled:opacity-60"
            >
              {savingEdit ? 'Saving Changes…' : 'Save Changes'}
            </button>
          </div>
        </form>
      ) : null}

      {loading ? <p className="mt-6 text-sm text-stone-600">Loading meetings for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && !error && meetings.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No meetings for this project.</p>
      ) : null}
      {!loading && !error && meetings.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {meetings.map((meeting) => (
            <li
              key={meeting.id}
              className="flex flex-col gap-3 p-4 transition-colors hover:bg-stone-50/50 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <Link
                    to={`/meetings/${meeting.id}`}
                    className="truncate text-sm font-semibold text-stone-950 hover:underline"
                  >
                    {meeting.title ?? 'Untitled meeting'}
                  </Link>
                  <span className="rounded bg-stone-100 px-1.5 py-0.5 text-[10px] font-mono text-stone-600">
                    ID: {meeting.id}
                  </span>
                </div>
                <div className="mt-1 flex items-center gap-3 text-xs text-stone-500">
                  <span>{meeting.meetingDate ?? 'No date'}</span>
                  <span>·</span>
                  <span>
                    {hasNotes(meeting)
                      ? `Notes available (${meeting.notes!.trim().length} chars)`
                      : 'No notes'}
                  </span>
                </div>
              </div>

              {/* Management Controls: Open, Edit, Delete */}
              <div className="flex items-center gap-2 shrink-0">
                <Link
                  to={`/meetings/${meeting.id}`}
                  className="rounded-md border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-700 shadow-2xs hover:bg-stone-100 hover:text-stone-950"
                >
                  Open
                </Link>
                <button
                  type="button"
                  onClick={() => startEdit(meeting)}
                  className="rounded-md border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-700 shadow-2xs hover:bg-stone-100 hover:text-stone-950"
                >
                  Edit
                </button>
                <button
                  type="button"
                  onClick={() => confirmDelete(meeting)}
                  className="rounded-md border border-red-200 bg-white px-2.5 py-1 text-xs font-medium text-red-600 shadow-2xs hover:bg-red-50 hover:border-red-300"
                >
                  Delete
                </button>
              </div>
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
