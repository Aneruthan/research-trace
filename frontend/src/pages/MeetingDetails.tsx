import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMeeting } from '../api/meetings'
import {
  acceptTaskSuggestion,
  extractTaskSuggestions,
  getTaskSuggestions,
  rejectTaskSuggestion,
  updateTaskSuggestion,
} from '../api/taskSuggestions'
import {
  extractResearch,
  getResearchReviews,
  submitResearchReview,
  type ExtractionReview,
  type MeetingExtraction,
  type ResearchReviewStatus,
} from '../api/research'
import type { Meeting, SuggestionUpdate, TaskSuggestion } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { SuggestionCard } from '../components/meetings/SuggestionCard'
import { ExtractionSections } from '../components/research/ExtractionSections'

type ExtractionTab = 'tasks' | 'research'

export function MeetingDetails() {
  const { meetingId: meetingIdParam } = useParams()
  const meetingId = Number(meetingIdParam)
  const meetingIdIsValid = Number.isInteger(meetingId) && meetingId > 0

  const [activeTab, setActiveTab] = useState<ExtractionTab>('tasks')

  const [meeting, setMeeting] = useState<Meeting | null>(null)
  const [meetingError, setMeetingError] = useState<string | null>(null)
  const [meetingLoading, setMeetingLoading] = useState(true)

  // Task Suggestions State
  const [suggestions, setSuggestions] = useState<TaskSuggestion[]>([])
  const [suggestionsError, setSuggestionsError] = useState<string | null>(null)
  const [suggestionsLoading, setSuggestionsLoading] = useState(true)
  const [extractingTasks, setExtractingTasks] = useState(false)

  // Research Extraction State
  const [researchReviews, setResearchReviews] = useState<ExtractionReview[]>([])
  const [researchReviewsLoading, setResearchReviewsLoading] = useState(false)
  const [researchExtraction, setResearchExtraction] = useState<MeetingExtraction | null>(null)
  const [extractingResearch, setExtractingResearch] = useState(false)
  const [submittingReview, setSubmittingReview] = useState<ResearchReviewStatus | null>(null)

  // Action Banners
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const loadResearchReviews = useCallback(async (signal?: AbortSignal) => {
    if (!meetingIdIsValid) return
    setResearchReviewsLoading(true)
    try {
      const reviews = await getResearchReviews(meetingId, signal)
      if (!signal?.aborted) {
        setResearchReviews(reviews)
      }
    } catch {
      // Research reviews are optional; ignore initial error if none
    } finally {
      if (!signal?.aborted) {
        setResearchReviewsLoading(false)
      }
    }
  }, [meetingId, meetingIdIsValid])

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

    loadResearchReviews(controller.signal)

    return () => controller.abort()
  }, [meetingId, meetingIdIsValid, loadResearchReviews])

  async function refreshSuggestions() {
    const data = await getTaskSuggestions(meetingId)
    setSuggestions(data)
    setSuggestionsError(null)
  }

  async function handleExtractTasks() {
    setExtractingTasks(true)
    setActionError(null)
    setActionMessage(null)
    try {
      await extractTaskSuggestions(meetingId)
      await refreshSuggestions()
      setActionMessage('Task suggestions updated from the meeting notes.')
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not extract tasks')
    } finally {
      setExtractingTasks(false)
    }
  }

  async function handleSaveTask(suggestionId: number, update: SuggestionUpdate) {
    setActionError(null)
    setActionMessage(null)
    await updateTaskSuggestion(suggestionId, update)
    await refreshSuggestions()
    setActionMessage('Suggestion updated.')
  }

  async function handleAcceptTask(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    await acceptTaskSuggestion(suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion accepted.')
  }

  async function handleRejectTask(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    await rejectTaskSuggestion(suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion rejected.')
  }

  // Research Extraction Handlers
  async function handleExtractResearch() {
    setExtractingResearch(true)
    setActionError(null)
    setActionMessage(null)
    try {
      const data = await extractResearch(meetingId)
      setResearchExtraction(data)
      setActionMessage('Research entities extracted. Review suggestions below before persisting.')
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not extract research information')
    } finally {
      setExtractingResearch(false)
    }
  }

  async function handleReviewResearch(status: ResearchReviewStatus) {
    if (!researchExtraction) return
    setSubmittingReview(status)
    setActionError(null)
    setActionMessage(null)
    try {
      await submitResearchReview(meetingId, status, researchExtraction)
      setResearchExtraction(null)
      await loadResearchReviews()
      setActionMessage(
        status === 'ACCEPTED'
          ? 'Research extraction accepted and persisted to ResearchTrace.'
          : 'Research extraction review submitted.',
      )
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not submit research review')
    } finally {
      setSubmittingReview(null)
    }
  }

  return (
    <section>
      <Link to="/meetings" className="text-sm font-medium text-stone-600 hover:text-stone-950">
        Back to Meetings
      </Link>
      <div className="mt-4">
        <PageHeader
          title={meeting?.title ?? 'Meeting'}
          description="Transcript, extracted task suggestions, and research traceability."
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

      <div className="mt-8">
        <div className="flex border-b border-stone-200">
          <button
            type="button"
            className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === 'tasks'
                ? 'border-stone-950 text-stone-950'
                : 'border-transparent text-stone-500 hover:text-stone-700'
            }`}
            onClick={() => setActiveTab('tasks')}
          >
            Task Extraction ({suggestions.length})
          </button>
          <button
            type="button"
            className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === 'research'
                ? 'border-stone-950 text-stone-950'
                : 'border-transparent text-stone-500 hover:text-stone-700'
            }`}
            onClick={() => setActiveTab('research')}
          >
            Research Extraction ({researchReviews.length} {researchReviews.length === 1 ? 'review' : 'reviews'})
          </button>
        </div>

        {actionMessage ? (
          <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800">
            {actionMessage}
          </p>
        ) : null}
        {actionError ? <Alert message={actionError} /> : null}

        {activeTab === 'tasks' ? (
          <section className="mt-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-sm font-semibold text-stone-950">Task suggestions</h2>
                <p className="text-xs text-stone-500">Action items extracted from meeting notes for review.</p>
              </div>
              <button
                type="button"
                className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60"
                onClick={() => void handleExtractTasks()}
                disabled={!meetingIdIsValid || extractingTasks || meetingLoading || meetingError != null}
              >
                {extractingTasks ? 'Extracting…' : 'Extract Tasks with AI'}
              </button>
            </div>

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
                    onSave={handleSaveTask}
                    onAccept={handleAcceptTask}
                    onReject={handleRejectTask}
                    onError={(message) => {
                      setActionMessage(null)
                      setActionError(message)
                    }}
                  />
                ))}
              </ul>
            ) : null}
          </section>
        ) : (
          <section className="mt-6 space-y-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-sm font-semibold text-stone-950">Research extraction & review</h2>
                <p className="text-xs text-stone-500">
                  Decisions, experiments, and results extracted from meeting discussions.
                </p>
              </div>
              <div className="flex gap-2">
                <Link
                  to="/research-trace"
                  className="rounded-md border border-stone-300 px-3 py-2 text-sm font-medium text-stone-800 hover:bg-stone-50"
                >
                  View Research Trace
                </Link>
                <button
                  type="button"
                  className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60"
                  onClick={() => void handleExtractResearch()}
                  disabled={!meetingIdIsValid || extractingResearch || meetingLoading || meetingError != null}
                >
                  {extractingResearch ? 'Extracting Research…' : 'Extract Research with AI'}
                </button>
              </div>
            </div>

            {/* Live AI Extracted Suggestions (Unsaved) */}
            {researchExtraction ? (
              <div className="rounded-lg border-2 border-stone-800 bg-stone-50/50 p-5">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-900">
                        AI Extracted Suggestions (Unsaved)
                      </span>
                      <h3 className="text-sm font-semibold text-stone-950">Pending Human Review</h3>
                    </div>
                    <p className="mt-1 text-xs text-stone-600">
                      These suggestions have not yet been written to the permanent ResearchTrace database.
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-sm font-medium text-stone-800 hover:bg-stone-50 disabled:opacity-60"
                      onClick={() => void handleReviewResearch('REJECTED')}
                      disabled={submittingReview != null}
                    >
                      {submittingReview === 'REJECTED' ? 'Rejecting…' : 'Reject'}
                    </button>
                    <button
                      type="button"
                      className="rounded-md bg-stone-950 px-3 py-1.5 text-sm font-medium text-white hover:bg-stone-800 disabled:opacity-60"
                      onClick={() => void handleReviewResearch('ACCEPTED')}
                      disabled={submittingReview != null}
                    >
                      {submittingReview === 'ACCEPTED' ? 'Persisting…' : 'Accept & Persist'}
                    </button>
                  </div>
                </div>

                <div className="mt-5">
                  <ExtractionSections extraction={researchExtraction} />
                </div>
              </div>
            ) : null}

            {/* Existing Persisted Reviews for this meeting */}
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-stone-500">
                Persisted Meeting Reviews ({researchReviews.length})
              </h3>
              {researchReviewsLoading ? (
                <p className="mt-3 text-sm text-stone-600">Loading persisted reviews…</p>
              ) : researchReviews.length === 0 ? (
                <p className="mt-3 text-sm text-stone-500">
                  No persisted research reviews for this meeting yet. Run &quot;Extract Research with AI&quot; to begin.
                </p>
              ) : (
                <div className="mt-3 space-y-4">
                  {researchReviews.map((review) => {
                    let parsed: MeetingExtraction | null = null
                    try {
                      parsed = review.extractionJson ? JSON.parse(review.extractionJson) : null
                    } catch {
                      // ignore parse errors
                    }

                    return (
                      <article key={review.id} className="rounded-lg border border-stone-200 bg-white p-5">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span
                              className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                                review.status === 'ACCEPTED'
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : review.status === 'REJECTED'
                                  ? 'bg-red-100 text-red-800'
                                  : 'bg-stone-100 text-stone-800'
                              }`}
                            >
                              {review.status}
                            </span>
                            <span className="text-xs text-stone-500">
                              Review #{review.id} · {review.createdAt ?? 'Unknown date'}
                            </span>
                          </div>
                        </div>
                        {parsed ? (
                          <div className="mt-4">
                            <ExtractionSections extraction={parsed} />
                          </div>
                        ) : (
                          <p className="mt-2 text-xs text-stone-500">No structured details available.</p>
                        )}
                      </article>
                    )
                  })}
                </div>
              )}
            </div>
          </section>
        )}
      </div>
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
