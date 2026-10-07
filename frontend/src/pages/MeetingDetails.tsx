import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMeeting } from '../api/meetings'
import {
  acceptTaskSuggestion,
  extractTaskSuggestions,
  getTaskSuggestions,
  rejectTaskSuggestion,
  updateTaskSuggestion,
} from '../api/taskSuggestions'
import type { Meeting, SuggestionUpdate, TaskSuggestion } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { SuggestionCard } from '../components/meetings/SuggestionCard'

export function MeetingDetails() {
  const { meetingId: meetingIdParam } = useParams()
  const meetingId = Number(meetingIdParam)
  const meetingIdIsValid = Number.isInteger(meetingId) && meetingId > 0

  const [meeting, setMeeting] = useState<Meeting | null>(null)
  const [meetingError, setMeetingError] = useState<string | null>(null)
  const [meetingLoading, setMeetingLoading] = useState(true)

  const [suggestions, setSuggestions] = useState<TaskSuggestion[]>([])
  const [suggestionsError, setSuggestionsError] = useState<string | null>(null)
  const [suggestionsLoading, setSuggestionsLoading] = useState(true)

  const [extracting, setExtracting] = useState(false)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    if (!meetingIdIsValid) {
      setMeetingLoading(false)
      setSuggestionsLoading(false)
      setMeetingError('Meeting id is not valid')
      return
    }

    const controller = new AbortController()
    setMeetingLoading(true)
    setSuggestionsLoading(true)
    setMeetingError(null)
    setSuggestionsError(null)

    getMeeting(meetingId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setMeeting(data)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setMeetingError(cause instanceof Error ? cause.message : 'Could not load meeting')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setMeetingLoading(false)
        }
      })

    getTaskSuggestions(meetingId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setSuggestions(data)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setSuggestionsError(cause instanceof Error ? cause.message : 'Could not load task suggestions')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setSuggestionsLoading(false)
        }
      })

    return () => controller.abort()
  }, [meetingId, meetingIdIsValid])

  async function refreshSuggestions() {
    const data = await getTaskSuggestions(meetingId)
    setSuggestions(data)
    setSuggestionsError(null)
  }

  async function handleExtract() {
    setExtracting(true)
    setActionError(null)
    setActionMessage(null)
    try {
      await extractTaskSuggestions(meetingId)
      await refreshSuggestions()
      setActionMessage('Task suggestions updated from the meeting notes.')
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not extract tasks')
    } finally {
      setExtracting(false)
    }
  }

  async function handleSave(suggestionId: number, update: SuggestionUpdate) {
    setActionError(null)
    setActionMessage(null)
    await updateTaskSuggestion(suggestionId, update)
    await refreshSuggestions()
    setActionMessage('Suggestion updated.')
  }

  async function handleAccept(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    await acceptTaskSuggestion(suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion accepted.')
  }

  async function handleReject(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    await rejectTaskSuggestion(suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion rejected.')
  }

  return (
    <section>
      <Link to="/meetings" className="text-sm font-medium text-stone-600 hover:text-stone-950">
        Back to Meetings
      </Link>
      <div className="mt-4">
        <PageHeader
          title={meeting?.title ?? 'Meeting'}
          description="Transcript, extracted task suggestions, and human review."
        />
      </div>

      {meetingLoading ? <p className="mt-6 text-sm text-stone-600">Loading meeting…</p> : null}
      {!meetingLoading && meetingError ? <Alert message={meetingError} /> : null}

      {!meetingLoading && !meetingError && meeting ? (
        <article className="mt-6 rounded-lg border border-stone-200 bg-white p-5">
          <dl className="grid gap-4 sm:grid-cols-2">
            <div>
              <dt className="text-xs font-medium tracking-wide text-stone-500 uppercase">Date</dt>
              <dd className="mt-1 text-sm text-stone-900">{meeting.meetingDate ?? 'No date'}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium tracking-wide text-stone-500 uppercase">Project</dt>
              <dd className="mt-1 text-sm text-stone-900">
                {meeting.projectId != null ? `Project ${meeting.projectId}` : 'No project'}
              </dd>
            </div>
          </dl>
          <h2 className="mt-6 text-sm font-semibold text-stone-950">Notes</h2>
          {meeting.notes != null && meeting.notes.trim().length > 0 ? (
            <pre className="mt-2 whitespace-pre-wrap font-sans text-sm leading-6 text-stone-700">
              {meeting.notes}
            </pre>
          ) : (
            <p className="mt-2 text-sm text-stone-500">No notes for this meeting.</p>
          )}
        </article>
      ) : null}

      <section className="mt-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-sm font-semibold text-stone-950">Task suggestions</h2>
          <button
            type="button"
            className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60"
            onClick={() => void handleExtract()}
            disabled={!meetingIdIsValid || extracting || meetingLoading || meetingError != null}
          >
            {extracting ? 'Extracting…' : 'Extract Tasks with AI'}
          </button>
        </div>
        {actionMessage ? (
          <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800">
            {actionMessage}
          </p>
        ) : null}
        {actionError ? <Alert message={actionError} /> : null}
        {suggestionsLoading ? <p className="mt-4 text-sm text-stone-600">Loading suggestions…</p> : null}
        {!suggestionsLoading && suggestionsError ? <Alert message={suggestionsError} /> : null}
        {!suggestionsLoading && !suggestionsError && suggestions.length === 0 ? (
          <p className="mt-4 text-sm text-stone-500">No task suggestions for this meeting.</p>
        ) : null}
        {!suggestionsLoading && !suggestionsError && suggestions.length > 0 ? (
          <ul className="mt-4 space-y-4">
            {suggestions.map((suggestion) => (
              <SuggestionCard
                key={suggestion.id ?? suggestion.title}
                suggestion={suggestion}
                onSave={handleSave}
                onAccept={handleAccept}
                onReject={handleReject}
                onError={(message) => {
                  setActionMessage(null)
                  setActionError(message)
                }}
              />
            ))}
          </ul>
        ) : null}
      </section>
    </section>
  )
}

function Alert({ message }: { message: string }) {
  return (
    <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      {message}
    </p>
  )
}
