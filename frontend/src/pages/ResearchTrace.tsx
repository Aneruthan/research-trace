import { useEffect, useState } from 'react'
import { getProjectMeetings } from '../api/meetings'
import {
  ApiError,
  extractResearch,
  getProjectExperiments,
  getProjectResearchDecisions,
  getProjectResearchEvents,
  getProjectResults,
  getResearchReviews,
  submitResearchReview,
  type Experiment,
  type ExtractionReview,
  type MeetingExtraction,
  type ResearchDecision,
  type ResearchEvent,
  type ResearchReviewStatus,
  type Result,
} from '../api/research'
import type { Meeting } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { ExtractionSections } from '../components/research/ExtractionSections'
import { PersistedResearch } from '../components/research/PersistedResearch'

const SELECTED_PROJECT_ID = 1

const REVIEW_STATUSES: ResearchReviewStatus[] = ['PENDING', 'ACCEPTED', 'EDITED', 'REJECTED']

export function ResearchTrace() {
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [meetingsError, setMeetingsError] = useState<string | null>(null)
  const [meetingsLoading, setMeetingsLoading] = useState(true)
  const [meetingId, setMeetingId] = useState<number | null>(null)

  const [extraction, setExtraction] = useState<MeetingExtraction | null>(null)
  const [extracting, setExtracting] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState<ResearchReviewStatus | null>(null)

  const [reviews, setReviews] = useState<ExtractionReview[]>([])
  const [reviewsLoading, setReviewsLoading] = useState(false)
  const [reviewsError, setReviewsError] = useState<string | null>(null)

  const [decisions, setDecisions] = useState<ResearchDecision[]>([])
  const [experiments, setExperiments] = useState<Experiment[]>([])
  const [results, setResults] = useState<Result[]>([])
  const [events, setEvents] = useState<ResearchEvent[]>([])
  const [recordsLoading, setRecordsLoading] = useState(true)
  const [recordsError, setRecordsError] = useState<string | null>(null)
  const [recordsReload, setRecordsReload] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getProjectMeetings(SELECTED_PROJECT_ID, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setMeetings(data)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setMeetingsError(cause instanceof Error ? cause.message : 'Could not load meetings')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setMeetingsLoading(false)
        }
      })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (meetingId == null) {
      setReviews([])
      return
    }
    const controller = new AbortController()
    setReviewsLoading(true)
    setReviewsError(null)
    getResearchReviews(meetingId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setReviews(data)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setReviewsError(cause instanceof Error ? cause.message : 'Could not load research reviews')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setReviewsLoading(false)
        }
      })
    return () => controller.abort()
  }, [meetingId])

  useEffect(() => {
    const controller = new AbortController()
    setRecordsLoading(true)
    setRecordsError(null)
    Promise.all([
      getProjectResearchDecisions(SELECTED_PROJECT_ID, controller.signal),
      getProjectExperiments(SELECTED_PROJECT_ID, controller.signal),
      getProjectResults(SELECTED_PROJECT_ID, controller.signal),
      getProjectResearchEvents(SELECTED_PROJECT_ID, controller.signal),
    ])
      .then(([loadedDecisions, loadedExperiments, loadedResults, loadedEvents]) => {
        if (!controller.signal.aborted) {
          setDecisions(loadedDecisions)
          setExperiments(loadedExperiments)
          setResults(loadedResults)
          setEvents(loadedEvents)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setRecordsError(cause instanceof Error ? cause.message : 'Could not load persisted research records')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setRecordsLoading(false)
        }
      })
    return () => controller.abort()
  }, [recordsReload])

  async function handleExtract() {
    if (meetingId == null) {
      return
    }
    setExtracting(true)
    setActionError(null)
    setActionMessage(null)
    setExtraction(null)
    try {
      const result = await extractResearch(meetingId)
      setExtraction(result)
      setActionMessage('Research extraction returned by the backend.')
    } catch (cause: unknown) {
      setExtraction(null)
      setActionError(cause instanceof ApiError || cause instanceof Error
        ? cause.message
        : 'Could not extract research information')
    } finally {
      setExtracting(false)
    }
  }

  async function handleReview(status: ResearchReviewStatus) {
    if (meetingId == null || extraction == null) {
      return
    }
    setSubmitting(status)
    setActionError(null)
    setActionMessage(null)
    try {
      await submitResearchReview(meetingId, status, extraction)
      setReviews(await getResearchReviews(meetingId))
      setRecordsReload((value) => value + 1)
      setActionMessage(status === 'ACCEPTED'
        ? 'Research review saved as ACCEPTED. Persisted records were refreshed. A record appears only when that extraction has enough information to store.'
        : `Research review saved as ${status}. Review history was updated. This status does not create persisted research records.`)
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not save the research review')
    } finally {
      setSubmitting(null)
    }
  }

  return (
    <section>
      <PageHeader
        title="Research Trace"
        description="Extract research information from a stored meeting, save a review, and read the research records stored for this project."
      />
      <p className="mt-4 text-sm text-stone-600">
        Task suggestions stay on the meeting page. Saving a review as ACCEPTED asks the backend to store supported decisions, experiments, results, and timeline events. Other statuses update review history only.
      </p>

      {meetingsLoading ? <p className="mt-6 text-sm text-stone-600">Loading meetings…</p> : null}
      {meetingsError ? <Alert message={meetingsError} /> : null}
      {!meetingsLoading && !meetingsError && meetings.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No meetings for this project.</p>
      ) : null}

      {!meetingsLoading && meetings.length > 0 ? (
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="block text-xs font-medium text-stone-600">
            Meeting
            <select
              className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900 sm:w-80"
              value={meetingId ?? ''}
              onChange={(event) => {
                const value = event.target.value
                setMeetingId(value === '' ? null : Number(value))
                setExtraction(null)
                setActionError(null)
                setActionMessage(null)
              }}
            >
              <option value="">Select a meeting</option>
              {meetings.map((meeting) => (
                <option key={meeting.id} value={meeting.id ?? ''}>
                  {meeting.title ?? 'Untitled meeting'} ({meeting.meetingDate ?? 'no date'})
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60"
            disabled={meetingId == null || extracting}
            onClick={() => void handleExtract()}
          >
            {extracting ? 'Extracting…' : 'Extract research information'}
          </button>
        </div>
      ) : null}

      {actionMessage ? (
        <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800">
          {actionMessage}
        </p>
      ) : null}
      {actionError ? <Alert message={actionError} /> : null}

      <section className="mt-8">
        <h2 className="text-sm font-semibold text-stone-950">Extracted research information</h2>
        <p className="mt-2 text-sm text-stone-600">
          This is the current extractor response. It is not a persisted research record until you save it as ACCEPTED.
        </p>
        {extraction ? (
          <div className="mt-4 space-y-4">
            <ExtractionSections extraction={extraction} />
            <div className="flex flex-wrap gap-2">
              {REVIEW_STATUSES.map((status) => (
                <button
                  key={status}
                  type="button"
                  className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-sm font-medium text-stone-800 disabled:opacity-60"
                  disabled={submitting != null}
                  onClick={() => void handleReview(status)}
                >
                  {submitting === status ? 'Saving…' : `Save as ${status}`}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <p className="mt-3 text-sm text-stone-500">Select a meeting and extract to see the response here.</p>
        )}
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold text-stone-950">Review history</h2>
        <p className="mt-2 text-sm text-stone-600">
          Each saved review keeps its status, timestamp, and extraction JSON. That status is not the same as a stored decision, experiment, result, or event.
        </p>
        {meetingId == null ? <p className="mt-3 text-sm text-stone-500">Select a meeting to see its reviews.</p> : null}
        {reviewsLoading ? <p className="mt-3 text-sm text-stone-600">Loading reviews…</p> : null}
        {reviewsError ? <Alert message={reviewsError} /> : null}
        {meetingId != null && !reviewsLoading && !reviewsError && reviews.length === 0 ? (
          <p className="mt-3 text-sm text-stone-500">No saved research reviews for this meeting.</p>
        ) : null}
        {reviews.length > 0 ? (
          <ul className="mt-3 space-y-3">
            {reviews.map((review) => (
              <li key={review.id ?? review.createdAt} className="rounded-lg border border-stone-200 bg-white p-4">
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <p className="text-sm font-medium text-stone-950">{review.status ?? 'No status'}</p>
                  <p className="text-xs text-stone-500">{review.createdAt ?? 'No timestamp'}</p>
                </div>
                <p className="mt-3 text-xs font-medium text-stone-500">Stored extraction</p>
                <StoredExtraction json={review.extractionJson} />
              </li>
            ))}
          </ul>
        ) : null}
      </section>

      <PersistedResearch
        projectId={SELECTED_PROJECT_ID}
        loading={recordsLoading}
        error={recordsError}
        decisions={decisions}
        experiments={experiments}
        results={results}
        events={events}
      />
    </section>
  )
}

function StoredExtraction({ json }: { json: string | null }) {
  if (json == null || json.trim() === '') {
    return <p className="mt-2 text-sm text-stone-500">No extraction JSON.</p>
  }
  try {
    const parsed = JSON.parse(json) as MeetingExtraction
    return (
      <div className="mt-3">
        <ExtractionSections extraction={parsed} />
      </div>
    )
  } catch {
    return <pre className="mt-2 overflow-x-auto text-xs text-stone-700">{json}</pre>
  }
}

function Alert({ message }: { message: string }) {
  return (
    <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      {message}
    </p>
  )
}
