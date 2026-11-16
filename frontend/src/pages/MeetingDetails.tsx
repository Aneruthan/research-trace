import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMeeting, updateMeeting } from '../api/meetings'
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
import { getProjectMembers, getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'
import type { StudentOption } from '../components/meetings/suggestionForm'
import { SuggestionCard } from '../components/meetings/SuggestionCard'
import { ExtractionSections } from '../components/research/ExtractionSections'
import { incompleteExtractionIssues } from '../components/research/researchDisplay'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { meetingBelongsToProject } from '../project/projectSelection'

type ExtractionTab = 'tasks' | 'research'

export function MeetingDetails() {
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
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
  const [students, setStudents] = useState<StudentOption[]>([])
  const [studentsUnavailable, setStudentsUnavailable] = useState(false)

  // Research Extraction State
  const [researchReviews, setResearchReviews] = useState<ExtractionReview[]>([])
  const [researchReviewsLoading, setResearchReviewsLoading] = useState(false)
  const [researchExtraction, setResearchExtraction] = useState<MeetingExtraction | null>(null)
  const [originalResearchExtraction, setOriginalResearchExtraction] = useState<MeetingExtraction | null>(null)
  const [extractingResearch, setExtractingResearch] = useState(false)
  const [submittingReview, setSubmittingReview] = useState<ResearchReviewStatus | null>(null)

  // Action Banners
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  // Edit Meeting State
  const [isEditing, setIsEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editDate, setEditDate] = useState('')
  const [editNotes, setEditNotes] = useState('')
  const [savingEdit, setSavingEdit] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)

  useEffect(() => {
    if (!meetingBelongsToProject(meeting?.projectId, activeProjectId)) {
      setStudents([])
      return
    }
    const projectId = meeting?.projectId
    if (projectId == null) {
      return
    }
    const controller = new AbortController()
    Promise.all([
      getProjectMembers(projectId, controller.signal),
      getUsers(controller.signal),
    ])
      .then(([members, users]) => {
        if (controller.signal.aborted) {
          return
        }
        const names = new Map(users.filter((user) => user.id != null).map((user) => [user.id, user.name]))
        setStudents(
          members
            .filter((member) => member.role === 'STUDENT' && member.userId != null)
            .map((member) => ({
              id: member.userId as number,
              name: names.get(member.userId) ?? '',
            })),
        )
        setStudentsUnavailable(false)
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted || (cause instanceof Error && cause.name === 'AbortError')) {
          return
        }
        setStudents([])
        setStudentsUnavailable(true)
      })
    return () => controller.abort()
  }, [meeting?.projectId, activeProjectId])

  function startEditing() {
    if (!meeting) return
    setEditTitle(meeting.title ?? '')
    setEditDate(meeting.meetingDate ?? '')
    setEditNotes(meeting.notes ?? '')
    setEditError(null)
    setIsEditing(true)
  }

  function cancelEditing() {
    setIsEditing(false)
    setEditError(null)
  }

  async function handleSaveMeeting(e: React.FormEvent) {
    e.preventDefault()
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
    setActionError(null)
    setActionMessage(null)

    try {
      if (activeProjectId == null) {
        setEditError('Select a project before editing a meeting.')
        return
      }
      const updated = await updateMeeting(activeProjectId, meetingId, {
        title: editTitle.trim(),
        meetingDate: editDate,
        notes: editNotes.trim() || undefined,
      })
      setMeeting(updated)
      setIsEditing(false)
      setActionMessage('Meeting notes updated successfully. You can now run AI task or research extraction below.')
    } catch (cause: unknown) {
      setEditError(cause instanceof Error ? cause.message : 'Could not save meeting changes')
    } finally {
      setSavingEdit(false)
    }
  }

  const loadResearchReviews = useCallback(async (signal?: AbortSignal) => {
    if (!meetingIdIsValid || activeProjectId == null) return
    setResearchReviewsLoading(true)
    try {
      const reviews = await getResearchReviews(activeProjectId, meetingId, signal)
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
  }, [meetingId, meetingIdIsValid, activeProjectId])

  useEffect(() => {
    if (projectsLoading) {
      setMeetingLoading(true)
      return
    }
    if (!meetingIdIsValid) {
      setMeetingLoading(false)
      setSuggestionsLoading(false)
      setMeetingError('Meeting id is not valid')
      return
    }
    if (projectsError || activeProjectId == null) {
      setMeeting(null)
      setSuggestions([])
      setMeetingLoading(false)
      setSuggestionsLoading(false)
      return
    }

    const controller = new AbortController()
    const projectId = activeProjectId
    setMeeting(null)
    setSuggestions([])
    setMeetingLoading(true)
    setSuggestionsLoading(true)
    setMeetingError(null)
    setSuggestionsError(null)

    getMeeting(projectId, meetingId, controller.signal)
      .then(async (data) => {
        if (controller.signal.aborted) {
          return
        }
        setMeeting(data)
        if (!meetingBelongsToProject(data.projectId, projectId)) {
          setSuggestions([])
          setSuggestionsLoading(false)
          return
        }
        const loaded = await getTaskSuggestions(projectId, meetingId, controller.signal)
        if (!controller.signal.aborted) {
          setSuggestions(loaded)
        }
        await loadResearchReviews(controller.signal)
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
          setSuggestionsLoading(false)
        }
      })

    return () => controller.abort()
  }, [meetingId, meetingIdIsValid, loadResearchReviews, activeProjectId, projectsLoading, projectsError])

  async function refreshSuggestions() {
    if (activeProjectId == null) return
    const data = await getTaskSuggestions(activeProjectId, meetingId)
    setSuggestions(data)
    setSuggestionsError(null)
  }

  async function handleExtractTasks() {
    setExtractingTasks(true)
    setActionError(null)
    setActionMessage(null)
    try {
      if (activeProjectId == null) return
      await extractTaskSuggestions(activeProjectId, meetingId)
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
    if (activeProjectId == null) return
    await updateTaskSuggestion(activeProjectId, meetingId, suggestionId, update)
    await refreshSuggestions()
    setActionMessage('Suggestion updated.')
  }

  async function handleAcceptTask(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    if (activeProjectId == null) return
    await acceptTaskSuggestion(activeProjectId, meetingId, suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion accepted.')
  }

  async function handleRejectTask(suggestionId: number) {
    setActionError(null)
    setActionMessage(null)
    if (activeProjectId == null) return
    await rejectTaskSuggestion(activeProjectId, meetingId, suggestionId)
    await refreshSuggestions()
    setActionMessage('Suggestion rejected.')
  }

  // Research Extraction Handlers
  async function handleExtractResearch() {
    setExtractingResearch(true)
    setActionError(null)
    setActionMessage(null)
    try {
      if (activeProjectId == null) return
      const data = await extractResearch(activeProjectId, meetingId)
      setResearchExtraction(data)
      setOriginalResearchExtraction(structuredClone(data))
      setActionMessage('Research entities extracted. Review suggestions below before persisting.')
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not extract research information')
    } finally {
      setExtractingResearch(false)
    }
  }

  async function handleReviewResearch(status: ResearchReviewStatus) {
    if (!researchExtraction) return
    if (status === 'ACCEPTED' && !window.confirm('Accept this version into the research trace? Accepted records are kept and are not replaced by a later acceptance of the same content.')) return
    if (status === 'REJECTED' && !window.confirm('Reject these suggestions? The review will be kept, and nothing will be added to the research trace.')) return
    setSubmittingReview(status)
    setActionError(null)
    setActionMessage(null)
    try {
      if (activeProjectId == null) return
      await submitResearchReview(activeProjectId, meetingId, status, researchExtraction, originalResearchExtraction)
      setResearchExtraction(null)
      setOriginalResearchExtraction(null)
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
          title={meetingBelongsToProject(meeting?.projectId, activeProjectId) ? (meeting?.title ?? 'Meeting') : 'Meeting'}
          description="Transcript, extracted task suggestions, and research traceability."
        />
      </div>

      {projectsLoading || meetingLoading ? <p className="mt-6 text-sm text-stone-600">Loading meeting…</p> : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}
      {!meetingLoading && meetingError ? <Alert message={meetingError} /> : null}
      {!projectsLoading && !meetingLoading && !meetingError && meeting && !meetingBelongsToProject(meeting.projectId, activeProjectId) ? (
        <p className="mt-6 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900" role="alert">
          This meeting belongs to a different project. Open Meetings for the active project, or switch projects before viewing it.
        </p>
      ) : null}

      {!projectsLoading && !meetingLoading && !meetingError && meeting && meetingBelongsToProject(meeting.projectId, activeProjectId) ? (
        <article className="mt-6 rounded-lg border border-stone-200 bg-white p-5">
          {!isEditing ? (
            <>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-stone-100 pb-3">
                <dl className="grid grid-cols-2 gap-4">
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
                <button
                  type="button"
                  onClick={startEditing}
                  className="self-start rounded-md border border-stone-300 px-3 py-1.5 text-xs font-medium text-stone-800 transition-colors hover:bg-stone-50 sm:self-center"
                >
                  Edit Meeting / Notes
                </button>
              </div>
              <h2 className="mt-4 text-sm font-semibold text-stone-950">Notes / Transcript</h2>
              {meeting.notes != null && meeting.notes.trim().length > 0 ? (
                <pre className="mt-2 whitespace-pre-wrap font-sans text-sm leading-6 text-stone-700">
                  {meeting.notes}
                </pre>
              ) : (
                <p className="mt-2 text-sm text-stone-500">No notes for this meeting. Click "Edit Meeting / Notes" to add transcript or notes.</p>
              )}
            </>
          ) : (
            <form onSubmit={(e) => void handleSaveMeeting(e)}>
              <div className="flex items-center justify-between border-b border-stone-200 pb-3">
                <h2 className="text-sm font-semibold tracking-tight text-stone-950">Edit Meeting Details & Notes</h2>
                <button
                  type="button"
                  onClick={cancelEditing}
                  className="text-xs text-stone-500 hover:text-stone-800"
                >
                  Cancel
                </button>
              </div>

              {editError ? <div className="mt-3"><Alert message={editError} /></div> : null}

              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="edit-title" className="block text-xs font-medium text-stone-700">
                    Title <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="edit-title"
                    type="text"
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                    disabled={savingEdit}
                    required
                  />
                </div>
                <div>
                  <label htmlFor="edit-date" className="block text-xs font-medium text-stone-700">
                    Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="edit-date"
                    type="date"
                    value={editDate}
                    onChange={(e) => setEditDate(e.target.value)}
                    className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                    disabled={savingEdit}
                    required
                  />
                </div>
              </div>

              <div className="mt-4">
                <label htmlFor="edit-notes" className="block text-xs font-medium text-stone-700">
                  Meeting Notes / Transcript
                </label>
                <textarea
                  id="edit-notes"
                  rows={10}
                  value={editNotes}
                  onChange={(e) => setEditNotes(e.target.value)}
                  placeholder="Enter meeting discussion, research decisions, experiments, and action items..."
                  className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 font-mono text-sm text-stone-900 focus:border-stone-950 focus:outline-hidden"
                  disabled={savingEdit}
                />
              </div>

              <div className="mt-4 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={cancelEditing}
                  className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
                  disabled={savingEdit}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingEdit}
                  className="rounded-md bg-stone-950 px-4 py-1.5 text-sm font-medium text-white hover:bg-stone-800 disabled:opacity-60"
                >
                  {savingEdit ? 'Saving…' : 'Save Changes'}
                </button>
              </div>
            </form>
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
                    students={students}
                    studentsUnavailable={studentsUnavailable}
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
                      className="rounded-md bg-indigo-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-800 disabled:opacity-60"
                      onClick={() => void handleReviewResearch('EDITED')}
                      disabled={submittingReview != null}
                    >
                      {submittingReview === 'EDITED' ? 'Saving…' : 'Save as Edited'}
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
                  {incompleteExtractionIssues(researchExtraction).length > 0 ? (
                    <ul className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-900" role="status">
                      {incompleteExtractionIssues(researchExtraction).map((issue) => <li key={issue}>{issue}</li>)}
                    </ul>
                  ) : null}
                  <ExtractionSections extraction={researchExtraction} onChange={setResearchExtraction} />
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
