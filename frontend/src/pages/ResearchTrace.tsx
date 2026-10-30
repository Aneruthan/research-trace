import { useEffect, useState } from 'react'
import { getProjectMeetings } from '../api/meetings'
import {
  extractResearch,
  getDecisionLineage,
  getResearchReviews,
  getResearchSummary,
  submitResearchReview,
  supersedeResearchDecision,
  updateResearchChangeStatus,
  type DecisionLineageView,
  type ExtractionReview,
  type MeetingExtraction,
  type ResearchChange,
  type ResearchChangeStatus,
  type ResearchDecision,
  type ResearchEvent,
  type ResearchReviewStatus,
  type ResearchTraceSummary,
} from '../api/research'
import type { Meeting } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { ExtractionSections } from '../components/research/ExtractionSections'

const SELECTED_PROJECT_ID = 1

type ActiveTab = 'overview' | 'timeline' | 'decisions' | 'changes' | 'extraction'

export function ResearchTrace() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('overview')

  // Project & Meetings
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [meetingsLoading, setMeetingsLoading] = useState(true)
  const [meetingsError, setMeetingsError] = useState<string | null>(null)
  const [meetingId, setMeetingId] = useState<number | null>(null)

  // Summary & Trace Data
  const [summary, setSummary] = useState<ResearchTraceSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(true)
  const [timeline, setTimeline] = useState<ResearchEvent[]>([])
  const [decisions, setDecisions] = useState<ResearchDecision[]>([])
  const [changes, setChanges] = useState<ResearchChange[]>([])

  // Lineage Drilldown
  const [selectedDecisionId, setSelectedDecisionId] = useState<number | null>(null)
  const [lineage, setLineage] = useState<DecisionLineageView | null>(null)
  const [lineageLoading, setLineageLoading] = useState(false)

  // Supersede modal / form
  const [supersedeModalOpen, setSupersedeModalOpen] = useState(false)
  const [supersedeTarget, setSupersedeTarget] = useState<ResearchDecision | null>(null)
  const [supersedeTitle, setSupersedeTitle] = useState('')
  const [supersedeRationale, setSupersedeRationale] = useState('')
  const [supersedeSubmitting, setSupersedeSubmitting] = useState(false)

  // Extraction & Review
  const [extraction, setExtraction] = useState<MeetingExtraction | null>(null)
  const [extracting, setExtracting] = useState(false)
  const [reviews, setReviews] = useState<ExtractionReview[]>([])
  const [reviewsLoading, setReviewsLoading] = useState(false)
  const [submittingReview, setSubmittingReview] = useState<ResearchReviewStatus | null>(null)

  // Status banners
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Load Initial Project Summary & Meetings
  useEffect(() => {
    const controller = new AbortController()

    getProjectMeetings(SELECTED_PROJECT_ID, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setMeetings(data)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setMeetingsError(cause instanceof Error ? cause.message : 'Could not load meetings')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setMeetingsLoading(false)
      })

    refreshSummary(controller.signal)

    return () => controller.abort()
  }, [])

  // Load Reviews when Meeting changes
  useEffect(() => {
    if (meetingId == null) {
      setReviews([])
      return
    }
    const controller = new AbortController()
    setReviewsLoading(true)
    getResearchReviews(meetingId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setReviews(data)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setErrorMessage(cause instanceof Error ? cause.message : 'Could not load research reviews')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setReviewsLoading(false)
      })
    return () => controller.abort()
  }, [meetingId])

  async function refreshSummary(signal?: AbortSignal) {
    setSummaryLoading(true)
    try {
      const data = await getResearchSummary(SELECTED_PROJECT_ID, signal)
      setSummary(data)
      setDecisions(data.decisions ?? [])
      setTimeline(data.timeline ?? [])
      setChanges(data.changes ?? [])
    } catch (cause: unknown) {
      if (cause instanceof Error && cause.name !== 'AbortError') {
        setErrorMessage(cause.message)
      }
    } finally {
      setSummaryLoading(false)
    }
  }

  async function handleExtract() {
    if (meetingId == null) return
    setExtracting(true)
    setErrorMessage(null)
    setStatusMessage(null)
    setExtraction(null)
    try {
      const result = await extractResearch(meetingId)
      setExtraction(result)
      setStatusMessage('Research information extracted. Review suggestions below and confirm or edit.')
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not extract research information')
    } finally {
      setExtracting(false)
    }
  }

  async function handleReview(status: ResearchReviewStatus) {
    if (meetingId == null || extraction == null) return
    setSubmittingReview(status)
    setErrorMessage(null)
    setStatusMessage(null)
    try {
      await submitResearchReview(meetingId, status, extraction)
      setReviews(await getResearchReviews(meetingId))
      await refreshSummary()

      if (status === 'ACCEPTED' || status === 'EDITED') {
        setStatusMessage(`Research review saved as ${status}. Verified decisions, experiments, and results are now added to the research trace!`)
      } else {
        setStatusMessage(`Research review saved as ${status}. No records were added to the permanent trace.`)
      }
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not save the research review')
    } finally {
      setSubmittingReview(null)
    }
  }

  async function handleViewLineage(decisionId: number) {
    if (selectedDecisionId === decisionId && lineage) {
      setSelectedDecisionId(null)
      setLineage(null)
      return
    }
    setSelectedDecisionId(decisionId)
    setLineageLoading(true)
    try {
      const data = await getDecisionLineage(SELECTED_PROJECT_ID, decisionId)
      setLineage(data)
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not load decision lineage')
    } finally {
      setLineageLoading(false)
    }
  }

  function handleOpenSupersede(decision: ResearchDecision) {
    setSupersedeTarget(decision)
    setSupersedeTitle(`Pivot from ${decision.title}`)
    setSupersedeRationale('')
    setSupersedeModalOpen(true)
  }

  async function handleConfirmSupersede() {
    if (!supersedeTarget || !supersedeTitle.trim() || !supersedeRationale.trim()) {
      setErrorMessage('Please provide a title and rationale explaining the decision change.')
      return
    }
    setSupersedeSubmitting(true)
    setErrorMessage(null)
    try {
      await supersedeResearchDecision(SELECTED_PROJECT_ID, supersedeTarget.id, {
        title: supersedeTitle.trim(),
        rationale: supersedeRationale.trim(),
      })
      setStatusMessage(`Decision "${supersedeTarget.title}" successfully superseded by "${supersedeTitle.trim()}".`)
      setSupersedeModalOpen(false)
      setSupersedeTarget(null)
      await refreshSummary()
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not supersede decision')
    } finally {
      setSupersedeSubmitting(false)
    }
  }

  async function handleUpdateChangeStatus(changeId: number, newStatus: ResearchChangeStatus) {
    try {
      await updateResearchChangeStatus(SELECTED_PROJECT_ID, changeId, newStatus)
      setChanges((prev) =>
        prev.map((c) => (c.id === changeId ? { ...c, status: newStatus } : c)),
      )
      setStatusMessage(`Change status updated to ${newStatus}.`)
      await refreshSummary()
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not update change status')
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="Research Trace & Lineage"
        description="Trace research decisions, supporting experiments, results, and direction changes across supervision meetings."
      />

      {/* Metric Overview Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div className="rounded-lg border border-stone-200 bg-white p-4 shadow-xs">
          <p className="text-xs font-medium text-stone-500 uppercase tracking-wide">Decisions</p>
          <p className="mt-1 text-2xl font-bold text-stone-900">
            {summaryLoading ? '—' : (summary?.activeDecisions ?? 0)}
            <span className="text-xs font-normal text-stone-500"> / {summaryLoading ? '—' : (summary?.totalDecisions ?? 0)} active</span>
          </p>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white p-4 shadow-xs">
          <p className="text-xs font-medium text-stone-500 uppercase tracking-wide">Experiments</p>
          <p className="mt-1 text-2xl font-bold text-stone-900">
            {summaryLoading ? '—' : (summary?.completedExperiments ?? 0)}
            <span className="text-xs font-normal text-stone-500"> / {summaryLoading ? '—' : (summary?.totalExperiments ?? 0)} done</span>
          </p>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white p-4 shadow-xs">
          <p className="text-xs font-medium text-stone-500 uppercase tracking-wide">Results & Metrics</p>
          <p className="mt-1 text-2xl font-bold text-stone-900">{summaryLoading ? '—' : (summary?.totalResults ?? 0)}</p>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white p-4 shadow-xs">
          <p className="text-xs font-medium text-stone-500 uppercase tracking-wide">Flagged Shifts</p>
          <p className={`mt-1 text-2xl font-bold ${summary && summary.flaggedChanges > 0 ? 'text-amber-600' : 'text-stone-900'}`}>
            {summaryLoading ? '—' : (summary?.flaggedChanges ?? 0)}
            <span className="text-xs font-normal text-stone-500"> to review</span>
          </p>
        </div>
      </div>

      {/* Status Notifications */}
      {statusMessage && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          {statusMessage}
        </div>
      )}
      {errorMessage && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {errorMessage}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-stone-200">
        <nav className="flex space-x-6">
          <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} label="Overview & Trace" />
          <TabButton active={activeTab === 'decisions'} onClick={() => setActiveTab('decisions')} label={`Decisions (${decisions.length})`} />
          <TabButton active={activeTab === 'timeline'} onClick={() => setActiveTab('timeline')} label={`Timeline (${timeline.length})`} />
          <TabButton
            active={activeTab === 'changes'}
            onClick={() => setActiveTab('changes')}
            label={`Detected Changes (${changes.filter((c) => c.status === 'FLAGGED').length})`}
          />
          <TabButton active={activeTab === 'extraction'} onClick={() => setActiveTab('extraction')} label="AI Meeting Review" />
        </nav>
      </div>

      {/* TAB 1: OVERVIEW & TRACE */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <h3 className="text-base font-semibold text-stone-950">Scientific Traceability Lineage</h3>
            <p className="mt-1 text-sm text-stone-600">
              ResearchTrace answers: <em>Why was a research approach selected? Which experiment evaluated it? What results were obtained? Did later evidence supersede the decision?</em>
            </p>
            <div className="mt-4 grid gap-4 md:grid-cols-3">
              <div className="rounded-md border border-stone-100 bg-stone-50 p-3">
                <span className="text-xs font-bold text-stone-700 uppercase">1. Decision Rationale</span>
                <p className="mt-1 text-xs text-stone-600">
                  Every decision retains supervisor rationale, meeting provenance, and immutable history.
                </p>
              </div>
              <div className="rounded-md border border-stone-100 bg-stone-50 p-3">
                <span className="text-xs font-bold text-stone-700 uppercase">2. Evidence Linking</span>
                <p className="mt-1 text-xs text-stone-600">
                  Decisions guide specific experiments; experiments produce metrics and observations.
                </p>
              </div>
              <div className="rounded-md border border-stone-100 bg-stone-50 p-3">
                <span className="text-xs font-bold text-stone-700 uppercase">3. Non-Destructive Supersession</span>
                <p className="mt-1 text-xs text-stone-600">
                  Strategic pivots link backward to superseded decisions without overwriting past work.
                </p>
              </div>
            </div>
          </div>

          {/* Quick Active Decisions */}
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-stone-950">Active Research Decisions & Evidence</h3>
              <button
                type="button"
                className="text-xs font-medium text-stone-700 hover:text-stone-950 underline"
                onClick={() => setActiveTab('decisions')}
              >
                View all ({decisions.length})
              </button>
            </div>
            {decisions.filter((d) => d.status === 'ACTIVE').length === 0 ? (
              <p className="mt-3 text-sm text-stone-500">No active decisions recorded yet. Confirm an AI extraction or add one.</p>
            ) : (
              <div className="mt-4 space-y-3">
                {decisions
                  .filter((d) => d.status === 'ACTIVE')
                  .slice(0, 4)
                  .map((d) => (
                    <DecisionCard
                      key={d.id}
                      decision={d}
                      isSelected={selectedDecisionId === d.id}
                      onViewLineage={() => handleViewLineage(d.id)}
                      onSupersede={() => handleOpenSupersede(d)}
                    />
                  ))}
              </div>
            )}
          </div>

          {/* Inline Lineage Viewer */}
          {selectedDecisionId != null && (
            <LineageViewer
              lineage={lineage}
              loading={lineageLoading}
              onClose={() => {
                setSelectedDecisionId(null)
                setLineage(null)
              }}
            />
          )}
        </div>
      )}

      {/* TAB 2: ALL DECISIONS */}
      {activeTab === 'decisions' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-stone-950">All Research Decisions</h3>
              <p className="text-xs text-stone-600">Browse current active decisions and historical superseded decisions.</p>
            </div>
          </div>

          {decisions.length === 0 ? (
            <div className="rounded-lg border border-dashed border-stone-300 p-8 text-center text-sm text-stone-500">
              No decisions found. You can extract research decisions from meeting notes in the &ldquo;AI Meeting Review&rdquo; tab.
            </div>
          ) : (
            <div className="space-y-3">
              {decisions.map((d) => (
                <DecisionCard
                  key={d.id}
                  decision={d}
                  isSelected={selectedDecisionId === d.id}
                  onViewLineage={() => handleViewLineage(d.id)}
                  onSupersede={() => handleOpenSupersede(d)}
                />
              ))}
            </div>
          )}

          {selectedDecisionId != null && (
            <LineageViewer
              lineage={lineage}
              loading={lineageLoading}
              onClose={() => {
                setSelectedDecisionId(null)
                setLineage(null)
              }}
            />
          )}
        </div>
      )}

      {/* TAB 3: TIMELINE */}
      {activeTab === 'timeline' && (
        <div className="rounded-lg border border-stone-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-stone-950">Chronological Research Event Timeline</h3>
          <p className="mt-1 text-xs text-stone-600">
            Unified chronological sequence of meetings, decisions, experiments, results, and strategic shifts.
          </p>

          {timeline.length === 0 ? (
            <p className="mt-4 text-sm text-stone-500">No events recorded in the timeline yet.</p>
          ) : (
            <ol className="relative mt-6 border-l border-stone-200 ml-4 space-y-6">
              {timeline.map((event) => (
                <li key={event.id} className="ml-6">
                  <span className="absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full bg-stone-100 ring-4 ring-white">
                    <EventIcon type={event.eventType} />
                  </span>
                  <div className="rounded-lg border border-stone-200 bg-stone-50/50 p-3">
                    <div className="flex flex-wrap items-baseline justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <EventTypeBadge type={event.eventType} />
                        <h4 className="text-sm font-semibold text-stone-900">{event.title}</h4>
                      </div>
                      <time className="text-xs text-stone-500">{event.occurredAt?.replace('T', ' ').slice(0, 16)}</time>
                    </div>
                    {event.description && <p className="mt-2 text-xs text-stone-700">{event.description}</p>}
                    {event.sourceReference && (
                      <p className="mt-1 text-[11px] text-stone-500">
                        <span className="font-medium">Provenance:</span> {event.sourceReference}
                      </p>
                    )}
                    {event.researchDecisionId && (
                      <button
                        type="button"
                        className="mt-2 text-xs font-medium text-stone-700 hover:text-stone-950 underline"
                        onClick={() => {
                          setActiveTab('decisions')
                          handleViewLineage(event.researchDecisionId!)
                        }}
                      >
                        Inspect Decision Lineage &rarr;
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </div>
      )}

      {/* TAB 4: DETECTED CHANGES */}
      {activeTab === 'changes' && (
        <div className="space-y-4">
          <div>
            <h3 className="text-sm font-semibold text-stone-950">Detected Research Direction Changes</h3>
            <p className="text-xs text-stone-600">
              The AI change detection engine flags potential shifts in datasets, models, or hypotheses across meetings for supervisor review.
            </p>
          </div>

          {changes.length === 0 ? (
            <div className="rounded-lg border border-dashed border-stone-300 p-8 text-center text-sm text-stone-500">
              No direction shifts or contradictions detected across meetings.
            </div>
          ) : (
            <div className="space-y-3">
              {changes.map((change) => (
                <div key={change.id} className="rounded-lg border border-stone-200 bg-white p-4 shadow-xs">
                  <div className="flex flex-wrap items-baseline justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="rounded bg-stone-100 px-2 py-0.5 text-xs font-semibold text-stone-800">
                        {change.changeType}
                      </span>
                      <h4 className="text-sm font-medium text-stone-900">{change.title}</h4>
                    </div>
                    <ChangeStatusBadge status={change.status} />
                  </div>

                  <div className="mt-3 grid gap-2 sm:grid-cols-2 text-xs">
                    <div className="rounded bg-red-50/50 border border-red-100 p-2 text-stone-700">
                      <span className="font-semibold text-red-800">Previous Approach:</span>
                      <p className="mt-0.5">{change.previousApproach ?? 'None specified'}</p>
                    </div>
                    <div className="rounded bg-emerald-50/50 border border-emerald-100 p-2 text-stone-700">
                      <span className="font-semibold text-emerald-800">New Approach:</span>
                      <p className="mt-0.5">{change.newApproach ?? 'None specified'}</p>
                    </div>
                  </div>

                  {change.reason && (
                    <p className="mt-2 text-xs text-stone-600">
                      <span className="font-medium text-stone-800">Reason / Evidence:</span> {change.reason}
                    </p>
                  )}

                  <div className="mt-3 flex items-center justify-between border-t border-stone-100 pt-2 text-[11px] text-stone-500">
                    <span>{change.sourceReference ?? 'Source meeting'}</span>
                    <div className="flex gap-2">
                      {change.status === 'FLAGGED' && (
                        <>
                          <button
                            type="button"
                            className="rounded border border-stone-300 bg-white px-2 py-1 font-medium text-stone-700 hover:bg-stone-50"
                            onClick={() => handleUpdateChangeStatus(change.id, 'REVIEWED')}
                          >
                            Mark Reviewed
                          </button>
                          <button
                            type="button"
                            className="rounded border border-stone-300 bg-white px-2 py-1 font-medium text-stone-500 hover:bg-stone-50"
                            onClick={() => handleUpdateChangeStatus(change.id, 'DISMISSED')}
                          >
                            Dismiss
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 5: AI EXTRACTION & REVIEW */}
      {activeTab === 'extraction' && (
        <div className="space-y-6">
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <h3 className="text-sm font-semibold text-stone-950">AI Meeting Extraction & Human Review</h3>
            <p className="mt-1 text-xs text-stone-600">
              Extract structured research decisions, experiments, and results from stored notes.
              <strong> Human-in-the-loop:</strong> Suggestions remain unverified until accepted or edited.
            </p>

            {meetingsLoading ? <p className="mt-4 text-xs text-stone-600">Loading meetings…</p> : null}
            {meetingsError ? <p className="mt-4 text-xs text-red-600">{meetingsError}</p> : null}

            {!meetingsLoading && meetings.length > 0 && (
              <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                <label className="block text-xs font-medium text-stone-700">
                  Select Meeting
                  <select
                    className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900 sm:w-80"
                    value={meetingId ?? ''}
                    onChange={(event) => {
                      const value = event.target.value
                      setMeetingId(value === '' ? null : Number(value))
                      setExtraction(null)
                    }}
                  >
                    <option value="">Choose a meeting…</option>
                    {meetings.map((m) => (
                      <option key={m.id} value={m.id ?? ''}>
                        {m.title ?? 'Untitled'} ({m.meetingDate ?? 'No date'})
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  className="rounded-md bg-stone-950 px-4 py-2 text-sm font-medium text-white hover:bg-stone-800 disabled:opacity-50"
                  disabled={meetingId == null || extracting}
                  onClick={() => void handleExtract()}
                >
                  {extracting ? 'Extracting…' : 'Extract Research Notes'}
                </button>
              </div>
            )}
          </div>

          {/* Extracted Output for Review */}
          {extraction && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-semibold text-stone-950">Candidate Suggestions for Verification</h4>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-md bg-emerald-700 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-800 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('ACCEPTED')}
                  >
                    {submittingReview === 'ACCEPTED' ? 'Promoting…' : 'Accept & Promote to Trace'}
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-xs font-medium text-stone-800 hover:bg-stone-50 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('EDITED')}
                  >
                    {submittingReview === 'EDITED' ? 'Saving…' : 'Save as Edited'}
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('REJECTED')}
                  >
                    {submittingReview === 'REJECTED' ? 'Rejecting…' : 'Reject Suggestions'}
                  </button>
                </div>
              </div>

              <ExtractionSections extraction={extraction} />
            </div>
          )}

          {/* Stored Extraction Reviews */}
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <h4 className="text-sm font-semibold text-stone-950">Review History for Meeting</h4>
            {meetingId == null ? (
              <p className="mt-2 text-xs text-stone-500">Select a meeting to inspect past reviews.</p>
            ) : reviewsLoading ? (
              <p className="mt-2 text-xs text-stone-500">Loading reviews…</p>
            ) : reviews.length === 0 ? (
              <p className="mt-2 text-xs text-stone-500">No saved reviews for this meeting.</p>
            ) : (
              <ul className="mt-3 space-y-3">
                {reviews.map((r) => (
                  <li key={r.id} className="rounded-md border border-stone-100 bg-stone-50 p-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-stone-900">{r.status}</span>
                      <span className="text-stone-500">{r.createdAt?.replace('T', ' ').slice(0, 16)}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* Supersede Modal */}
      {supersedeModalOpen && supersedeTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-4">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h3 className="text-base font-semibold text-stone-950">Supersede Research Decision</h3>
            <p className="mt-1 text-xs text-stone-600">
              You are superseding: <strong>&ldquo;{supersedeTarget.title}&rdquo;</strong>. The earlier decision will be retained with status <span className="font-semibold text-amber-700">SUPERSEDED</span>.
            </p>

            <div className="mt-4 space-y-3">
              <label className="block text-xs font-medium text-stone-700">
                New Decision Title
                <input
                  type="text"
                  className="mt-1 block w-full rounded border border-stone-300 px-3 py-1.5 text-xs text-stone-900"
                  value={supersedeTitle}
                  onChange={(e) => setSupersedeTitle(e.target.value)}
                  placeholder="e.g. Switch dataset to LitMedImage"
                />
              </label>

              <label className="block text-xs font-medium text-stone-700">
                Rationale / Justification for Change
                <textarea
                  className="mt-1 block w-full rounded border border-stone-300 px-3 py-1.5 text-xs text-stone-900"
                  rows={3}
                  value={supersedeRationale}
                  onChange={(e) => setSupersedeRationale(e.target.value)}
                  placeholder="Explain what experiment, result, or observation motivated this change..."
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                className="rounded px-3 py-1.5 text-xs font-medium text-stone-600 hover:bg-stone-100"
                onClick={() => setSupersedeModalOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="rounded bg-stone-950 px-3 py-1.5 text-xs font-medium text-white hover:bg-stone-800 disabled:opacity-50"
                disabled={supersedeSubmitting || !supersedeTitle.trim() || !supersedeRationale.trim()}
                onClick={() => void handleConfirmSupersede()}
              >
                {supersedeSubmitting ? 'Saving…' : 'Confirm Supersession'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

function TabButton({ active, onClick, label }: { active: boolean; onClick: () => void; label: string }) {
  return (
    <button
      type="button"
      className={`border-b-2 pb-3 text-sm font-medium transition-colors ${
        active ? 'border-stone-950 text-stone-950' : 'border-transparent text-stone-500 hover:text-stone-800'
      }`}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

function DecisionCard({
  decision,
  isSelected,
  onViewLineage,
  onSupersede,
}: {
  decision: ResearchDecision
  isSelected: boolean
  onViewLineage: () => void
  onSupersede: () => void
}) {
  const isSuper = decision.status === 'SUPERSEDED'
  return (
    <div className={`rounded-lg border p-4 transition-all ${isSelected ? 'border-stone-950 bg-stone-50/50' : 'border-stone-200 bg-white'}`}>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <div className="flex items-center gap-2">
          <span
            className={`rounded px-2 py-0.5 text-[11px] font-semibold ${
              isSuper ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
            }`}
          >
            {decision.status}
          </span>
          <h4 className="text-sm font-semibold text-stone-900">{decision.title}</h4>
        </div>
        <span className="text-xs text-stone-500">{decision.decidedAt ?? 'Undated'}</span>
      </div>

      {decision.decisionText && <p className="mt-2 text-xs text-stone-700">{decision.decisionText}</p>}
      {decision.rationale && (
        <p className="mt-1 text-xs text-stone-600">
          <span className="font-medium text-stone-800">Rationale:</span> {decision.rationale}
        </p>
      )}

      {decision.sourceReference && (
        <p className="mt-2 text-[11px] text-stone-500">
          <span className="font-medium">Provenance:</span> {decision.sourceReference}
        </p>
      )}

      <div className="mt-3 flex items-center justify-between border-t border-stone-100 pt-2 text-xs">
        <button
          type="button"
          className="font-medium text-stone-800 hover:text-stone-950 underline"
          onClick={onViewLineage}
        >
          {isSelected ? 'Hide Evidence Chain' : 'Trace Evidence Chain &rarr;'}
        </button>

        {!isSuper && (
          <button
            type="button"
            className="rounded border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-700 hover:bg-stone-50"
            onClick={onSupersede}
          >
            Supersede Approach
          </button>
        )}
      </div>
    </div>
  )
}

function LineageViewer({
  lineage,
  loading,
  onClose,
}: {
  lineage: DecisionLineageView | null
  loading: boolean
  onClose: () => void
}) {
  if (loading) {
    return (
      <div className="rounded-lg border border-stone-200 bg-white p-6 text-center text-sm text-stone-500">
        Reconstructing decision evidence lineage…
      </div>
    )
  }
  if (!lineage) return null

  return (
    <div className="rounded-lg border border-stone-950 bg-stone-50 p-5 shadow-xs">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-bold text-stone-950">Decision Evidence & Provenance Trace</h4>
        <button type="button" className="text-xs text-stone-500 hover:text-stone-950" onClick={onClose}>
          Close [&times;]
        </button>
      </div>

      <div className="mt-4 space-y-4">
        {/* Predecessor */}
        {lineage.predecessorDecision && (
          <div className="rounded-md border border-amber-200 bg-amber-50/60 p-3 text-xs">
            <span className="font-semibold text-amber-900 uppercase tracking-wide">Predecessor Decision (Superseded)</span>
            <p className="mt-1 font-medium text-stone-900">{lineage.predecessorDecision.title}</p>
            <p className="mt-0.5 text-stone-600">Rationale: {lineage.predecessorDecision.rationale}</p>
          </div>
        )}

        {/* Current Decision Focus */}
        <div className="rounded-md border border-stone-200 bg-white p-3 text-xs shadow-xs">
          <span className="font-semibold text-stone-900 uppercase tracking-wide">Decision Focus</span>
          <p className="mt-1 text-sm font-bold text-stone-950">{lineage.decision.title}</p>
          <p className="mt-1 text-stone-700">{lineage.decision.decisionText}</p>
          <p className="mt-1 text-stone-600"><span className="font-medium">Why selected:</span> {lineage.decision.rationale}</p>
          {lineage.sourceMeeting && (
            <p className="mt-1 text-stone-500">
              <span className="font-medium">Source Meeting:</span> {lineage.sourceMeeting.title} ({lineage.sourceMeeting.meetingDate})
            </p>
          )}
        </div>

        {/* Supporting Experiments and Results */}
        <div className="space-y-2">
          <span className="text-xs font-semibold text-stone-900 uppercase tracking-wide">
            Supporting Experiments & Evidence ({lineage.experiments.length})
          </span>
          {lineage.experiments.length === 0 ? (
            <p className="text-xs text-stone-500">No experiments directly linked to this decision yet.</p>
          ) : (
            lineage.experiments.map((ev, idx) => (
              <div key={idx} className="rounded-md border border-stone-200 bg-white p-3 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-stone-900">{ev.experiment.title}</span>
                  <span className="text-[11px] text-stone-500">{ev.experiment.status}</span>
                </div>
                <p className="mt-1 text-stone-600"><span className="font-medium">Hypothesis:</span> {ev.experiment.hypothesis}</p>

                {ev.results.length > 0 && (
                  <div className="mt-2 space-y-1 border-t border-stone-100 pt-2">
                    <span className="font-medium text-stone-700">Observed Results:</span>
                    {ev.results.map((r) => (
                      <div key={r.id} className="rounded bg-stone-50 p-2 text-stone-800">
                        <p className="font-medium">{r.summary}</p>
                        {r.dataOrMetric && <p className="text-[11px] text-stone-600">Metric: {r.dataOrMetric}</p>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Successor */}
        {lineage.successorDecision && (
          <div className="rounded-md border border-emerald-200 bg-emerald-50/60 p-3 text-xs">
            <span className="font-semibold text-emerald-900 uppercase tracking-wide">Subsequent Decision (Active Pivot)</span>
            <p className="mt-1 font-medium text-stone-900">{lineage.successorDecision.title}</p>
            <p className="mt-0.5 text-stone-600">Rationale: {lineage.successorDecision.rationale}</p>
          </div>
        )}
      </div>
    </div>
  )
}

function EventTypeBadge({ type }: { type: string }) {
  const map: Record<string, string> = {
    DECISION: 'bg-indigo-100 text-indigo-800',
    EXPERIMENT: 'bg-blue-100 text-blue-800',
    RESULT: 'bg-emerald-100 text-emerald-800',
    CHANGE: 'bg-amber-100 text-amber-800',
    MEETING: 'bg-stone-200 text-stone-800',
  }
  return <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${map[type] ?? 'bg-stone-100 text-stone-800'}`}>{type}</span>
}

function ChangeStatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = {
    FLAGGED: 'bg-amber-100 text-amber-800',
    REVIEWED: 'bg-emerald-100 text-emerald-800',
    DISMISSED: 'bg-stone-100 text-stone-600',
  }
  return <span className={`rounded px-2 py-0.5 text-[11px] font-semibold ${map[status] ?? 'bg-stone-100 text-stone-600'}`}>{status}</span>
}

function EventIcon({ type }: { type: string }) {
  switch (type) {
    case 'DECISION':
      return <span className="text-xs">&#9670;</span>
    case 'EXPERIMENT':
      return <span className="text-xs">&#9881;</span>
    case 'RESULT':
      return <span className="text-xs">&#10003;</span>
    case 'CHANGE':
      return <span className="text-xs">&#8644;</span>
    default:
      return <span className="text-xs">&#8226;</span>
  }
}
