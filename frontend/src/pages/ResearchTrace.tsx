import { useEffect, useRef, useState } from 'react'
import { getProjectMeetings } from '../api/meetings'
import {
  createProjectExperiment,
  extractResearch,
  getDecisionLineage,
  getResearchReviews,
  getResearchSummary,
  submitResearchReview,
  supersedeResearchDecision,
  updateExperimentDecision,
  updateResearchChangeStatus,
  type DecisionLineageView,
  type Experiment,
  type ExtractionReview,
  type MeetingExtraction,
  type ResearchChange,
  type ResearchChangeStatus,
  type ResearchDecision,
  type ResearchEvent,
  type ResearchReviewStatus,
  type ResearchTraceSummary,
  type Result,
} from '../api/research'
import type { Meeting } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { ExtractionSections } from '../components/research/ExtractionSections'
import { PersistedResearch } from '../components/research/PersistedResearch'
import { buildDecisionChains, extractionOrigin, incompleteExtractionIssues, reviewOutcomeMessage } from '../components/research/researchDisplay'

type ActiveTab = 'overview' | 'timeline' | 'decisions' | 'changes' | 'extraction'

export function ResearchTrace() {
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [activeTab, setActiveTab] = useState<ActiveTab>('overview')

  // Project & Meetings
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [meetingsLoading, setMeetingsLoading] = useState(true)
  const [meetingsError, setMeetingsError] = useState<string | null>(null)
  const [meetingId, setMeetingId] = useState<number | null>(null)

  // Summary & Trace Data
  const [summary, setSummary] = useState<ResearchTraceSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(true)
  const [summaryError, setSummaryError] = useState<string | null>(null)
  const [timeline, setTimeline] = useState<ResearchEvent[]>([])
  const [decisions, setDecisions] = useState<ResearchDecision[]>([])
  const [changes, setChanges] = useState<ResearchChange[]>([])
  const [experiments, setExperiments] = useState<Experiment[]>([])
  const [results, setResults] = useState<Result[]>([])

  // Lineage Drilldown
  const [selectedDecisionId, setSelectedDecisionId] = useState<number | null>(null)
  const [lineage, setLineage] = useState<DecisionLineageView | null>(null)
  const [lineageLoading, setLineageLoading] = useState(false)

  // Supersede modal / form
  const [supersedeModalOpen, setSupersedeModalOpen] = useState(false)
  const [supersedeTarget, setSupersedeTarget] = useState<ResearchDecision | null>(null)
  const [supersedeTitle, setSupersedeTitle] = useState('')
  const [supersedeRationale, setSupersedeRationale] = useState('')
  const [supersedeMeetingId, setSupersedeMeetingId] = useState<number | null>(null)
  const [supersedeError, setSupersedeError] = useState<string | null>(null)
  const [supersedeSubmitting, setSupersedeSubmitting] = useState(false)

  // Extraction & Review
  const [extraction, setExtraction] = useState<MeetingExtraction | null>(null)
  const [originalExtraction, setOriginalExtraction] = useState<MeetingExtraction | null>(null)
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
    setMeetings([])
    setSummary(null)
    setDecisions([])
    setTimeline([])
    setChanges([])
    setExperiments([])
    setResults([])
    setMeetingId(null)
    setExtraction(null)
    setOriginalExtraction(null)
    setLineage(null)
    setSelectedDecisionId(null)
    setMeetingsError(null)
    setSummaryError(null)
    if (projectsLoading) {
      setMeetingsLoading(true)
      setSummaryLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null) {
      setMeetingsLoading(false)
      setSummaryLoading(false)
      return () => controller.abort()
    }
    const projectId = activeProjectId

    getProjectMeetings(projectId, controller.signal)
      .then((data) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          return
        }
        setMeetings(data)
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          return
        }
        setMeetingsError(cause instanceof Error ? cause.message : 'Could not load meetings')
      })
      .finally(() => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setMeetingsLoading(false)
        }
      })

    void refreshSummary(projectId, controller.signal)

    return () => controller.abort()
  }, [activeProjectId, projectsLoading, projectsError])

  // Load Reviews when Meeting changes
  useEffect(() => {
    if (meetingId == null || activeProjectId == null) {
      setReviews([])
      return
    }
    const controller = new AbortController()
    const projectId = activeProjectId
    setReviewsLoading(true)
    getResearchReviews(projectId, meetingId, controller.signal)
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
  }, [meetingId, activeProjectId])

  async function refreshSummary(projectId: number | null, signal?: AbortSignal) {
    if (projectId == null) {
      return
    }
    setSummaryLoading(true)
    setSummaryError(null)
    try {
      const data = await getResearchSummary(projectId, signal)
      if (signal?.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
        return
      }
      setSummary(data)
      setDecisions(data.decisions ?? [])
      setTimeline(data.timeline ?? [])
      setChanges(data.changes ?? [])
      setExperiments(data.experiments ?? [])
      setResults(data.results ?? [])
    } catch (cause: unknown) {
      if (signal?.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
        return
      }
      if (cause instanceof Error && cause.name !== 'AbortError') {
        setErrorMessage(cause.message)
        setSummaryError(cause.message)
      }
    } finally {
      if (!signal?.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
        setSummaryLoading(false)
      }
    }
  }

  async function handleExtract() {
    if (meetingId == null) return
    setExtracting(true)
    setErrorMessage(null)
    setStatusMessage(null)
    setExtraction(null)
    setOriginalExtraction(null)
    try {
      if (activeProjectId == null) return
      const result = await extractResearch(activeProjectId, meetingId)
      setExtraction(result)
      setOriginalExtraction(structuredClone(result))
      setStatusMessage('Research information extracted. Review suggestions below and confirm or edit.')
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not extract research information')
    } finally {
      setExtracting(false)
    }
  }

  async function handleReview(status: ResearchReviewStatus) {
    if (meetingId == null || extraction == null) return
    if (status === 'ACCEPTED' && !window.confirm('Accept this version into the research trace? Accepted records are kept and are not replaced by a later acceptance of the same content.')) return
    if (status === 'REJECTED' && !window.confirm('Reject these suggestions? The review will be kept, and nothing will be added to the research trace.')) return
    setSubmittingReview(status)
    setErrorMessage(null)
    setStatusMessage(null)
    try {
      if (activeProjectId == null) return
      await submitResearchReview(activeProjectId, meetingId, status, extraction, originalExtraction)
      setExtraction(null)
      setOriginalExtraction(null)
      setReviews(await getResearchReviews(activeProjectId, meetingId))
      await refreshSummary(activeProjectId)
      setStatusMessage(reviewOutcomeMessage(status))
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not save the research review')
    } finally {
      setSubmittingReview(null)
    }
  }

  async function handleViewLineage(decisionId: number) {
    if (selectedDecisionId === decisionId) {
      setSelectedDecisionId(null)
      setLineage(null)
      return
    }
    if (activeProjectId == null) {
      return
    }
    setSelectedDecisionId(decisionId)
    setLineageLoading(true)
    try {
      const data = await getDecisionLineage(activeProjectId, decisionId)
      setLineage(data)
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not load decision lineage')
    } finally {
      setLineageLoading(false)
    }
  }

  function handleOpenSupersede(target: ResearchDecision) {
    setSupersedeTarget(target)
    setSupersedeTitle('')
    setSupersedeRationale('')
    setSupersedeMeetingId(null)
    setSupersedeError(null)
    setSupersedeModalOpen(true)
  }

  async function handleConfirmSupersede() {
    if (activeProjectId == null || !supersedeTarget || !supersedeTarget.id || !supersedeTitle.trim() || !supersedeRationale.trim()) return
    setSupersedeSubmitting(true)
    setSupersedeError(null)
    setStatusMessage(null)
    setErrorMessage(null)
    try {
      await supersedeResearchDecision(activeProjectId, supersedeTarget.id, {
        title: supersedeTitle.trim(),
        rationale: supersedeRationale.trim(),
        meetingId: supersedeMeetingId ?? undefined,
      })
      setSupersedeModalOpen(false)
      setSupersedeTarget(null)
      setStatusMessage(`Decision "${supersedeTarget.title}" was superseded by "${supersedeTitle.trim()}".`)
      await refreshSummary(activeProjectId)
    } catch (cause: unknown) {
      setSupersedeError(cause instanceof Error ? cause.message : 'Could not supersede decision')
    } finally {
      setSupersedeSubmitting(false)
    }
  }

  async function handleCreateExperiment(title: string, decisionId: number | null) {
    if (activeProjectId == null) {
      return
    }
    setErrorMessage(null)
    await createProjectExperiment(activeProjectId, {
      title,
      researchDecisionId: decisionId,
    })
    setStatusMessage(`Experiment "${title}" was added.`)
    await refreshSummary(activeProjectId)
  }

  async function handleLinkExperiment(experimentId: number, decisionId: number | null) {
    if (activeProjectId == null) {
      return
    }
    setErrorMessage(null)
    await updateExperimentDecision(activeProjectId, experimentId, decisionId)
    setStatusMessage(decisionId == null ? 'Experiment decision link cleared.' : 'Experiment linked to the selected decision.')
    await refreshSummary(activeProjectId)
  }

  async function handleUpdateChangeStatus(changeId: number, newStatus: ResearchChangeStatus) {
    if (activeProjectId == null) {
      return
    }
    try {
      await updateResearchChangeStatus(activeProjectId, changeId, newStatus)
      setChanges((prev) =>
        prev.map((c) => (c.id === changeId ? { ...c, status: newStatus } : c)),
      )
      setStatusMessage(`Change status updated to ${newStatus}.`)
      await refreshSummary(activeProjectId)
    } catch (cause: unknown) {
      setErrorMessage(cause instanceof Error ? cause.message : 'Could not update change status')
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="Research Trace & Lineage"
        description="Extract research information, review decisions, trace supporting experiments, results, and direction changes across supervision meetings."
      />
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}

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
          <p className="mt-1 text-2xl font-bold text-amber-700">{summaryLoading ? '—' : (summary?.flaggedChanges ?? 0)}</p>
        </div>
      </div>

      {/* Banners */}
      {statusMessage && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 p-4 text-xs font-medium text-emerald-800">
          {statusMessage}
        </div>
      )}
      {errorMessage && (
        <div className="rounded-md border border-red-200 bg-red-50 p-4 text-xs font-medium text-red-800">
          {errorMessage}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-stone-200">
        <nav className="flex space-x-6">
          <TabButton
            active={activeTab === 'overview'}
            onClick={() => setActiveTab('overview')}
            label="Lineage Tree"
          />
          <TabButton
            active={activeTab === 'decisions'}
            onClick={() => setActiveTab('decisions')}
            label={`Decisions (${decisions.length})`}
          />
          <TabButton
            active={activeTab === 'timeline'}
            onClick={() => setActiveTab('timeline')}
            label={`Timeline (${timeline.length})`}
          />
          <TabButton
            active={activeTab === 'changes'}
            onClick={() => setActiveTab('changes')}
            label={`Direction Changes (${changes.length})`}
          />
          <TabButton
            active={activeTab === 'extraction'}
            onClick={() => setActiveTab('extraction')}
            label="Meeting Extraction & Review"
          />
        </nav>
      </div>

      {/* Tab 1: Lineage Tree & Overview */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <h3 className="text-sm font-semibold text-stone-950">Decision Evolution & Pivot Tree</h3>
            <p className="mt-1 text-xs text-stone-600">
              Interactive visualization of active research directions and superseded hypotheses.
            </p>

            {summaryLoading ? (
              <p className="mt-4 text-xs text-stone-500">Loading trace summary…</p>
            ) : decisions.length === 0 ? (
              <p className="mt-4 text-xs text-stone-500">No decisions recorded yet. Run a meeting extraction to seed the tree.</p>
            ) : (
              <div className="mt-6 space-y-4">
                {buildDecisionChains(decisions).map(([root, ...successors]) => (
                  <div key={root.id} className="rounded-md border border-stone-200 bg-stone-50/50 p-4">
                    <div className="flex items-center gap-2">
                      <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${decisionStatusClass(root.status)}`}>
                        {root.status}
                      </span>
                      <span className="text-xs font-bold text-stone-900">{root.title}</span>
                      <span className="text-[11px] text-stone-500">({root.decidedAt ?? 'undated'})</span>
                    </div>
                    {root.rationale && <p className="mt-1 text-xs text-stone-600 pl-4 border-l border-stone-300">Rationale: {root.rationale}</p>}

                    {successors.length > 0 && (
                      <div className="mt-3 ml-6 space-y-2 border-l-2 border-indigo-200 pl-4">
                        {successors.map((successor) => (
                          <div key={successor.id} className="space-y-2">
                            <span className="text-[11px] font-semibold text-indigo-700">&darr; Pivoted to:</span>
                            <div className="rounded border border-indigo-100 bg-white p-3">
                              <div className="flex items-center gap-2">
                                <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${decisionStatusClass(successor.status)}`}>
                                  {successor.status}
                                </span>
                                <span className="text-xs font-bold text-stone-900">{successor.title}</span>
                              </div>
                              {successor.rationale && (
                                <p className="mt-1 text-xs text-stone-600">New Direction Rationale: {successor.rationale}</p>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 2: Decisions & Evidence Lineage */}
      {activeTab === 'decisions' && (
        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-stone-950">Recorded Research Decisions</h3>
              {summaryLoading ? (
                <p className="text-xs text-stone-500">Loading decisions…</p>
              ) : decisions.length === 0 ? (
                <p className="text-xs text-stone-500">No decisions recorded.</p>
              ) : (
                decisions.map((d) => (
                  <DecisionCard
                    key={d.id}
                    decision={d}
                    isSelected={selectedDecisionId === d.id}
                    onViewLineage={() => d.id != null && void handleViewLineage(d.id)}
                    onSupersede={() => handleOpenSupersede(d)}
                  />
                ))
              )}
            </div>

            <div>
              <LineageViewer
                lineage={lineage}
                loading={lineageLoading}
                onClose={() => {
                  setSelectedDecisionId(null)
                  setLineage(null)
                }}
              />
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Timeline */}
      {activeTab === 'timeline' && (
        <div className="rounded-lg border border-stone-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-stone-950">Chronological Research Timeline</h3>
          <p className="mt-1 text-xs text-stone-600">
            Integrated stream of meetings, decisions, experiments, observed results, and pivots.
          </p>

          {summaryLoading ? (
            <p className="mt-4 text-xs text-stone-500">Loading timeline…</p>
          ) : timeline.length === 0 ? (
            <p className="mt-4 text-xs text-stone-500">No events recorded in this project timeline.</p>
          ) : (
            <div className="mt-6 relative border-l-2 border-stone-200 ml-4 space-y-6 pl-6">
              {timeline.map((event) => (
                <div key={event.id} className="relative group">
                  <div className="absolute -left-[31px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-white bg-stone-900 group-hover:scale-125 transition-transform" />
                  <div className="flex flex-wrap items-baseline gap-2">
                    <EventTypeBadge type={event.eventType ?? 'MEETING'} />
                    <span className="text-xs font-semibold text-stone-900">{event.title}</span>
                    <span className="text-[11px] text-stone-500">{event.occurredAt?.slice(0, 10)}</span>
                    {event.taskId != null && (
                      <span className="rounded bg-sky-50 px-1.5 py-0.5 text-[10px] font-medium text-sky-700">
                        Task #{event.taskId}
                      </span>
                    )}
                    {event.researchDecisionId != null && (
                      <span className="rounded bg-indigo-50 px-1.5 py-0.5 text-[10px] font-medium text-indigo-700">
                        Decision #{event.researchDecisionId}
                      </span>
                    )}
                    {event.experimentId != null && (
                      <span className="rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-700">
                        Experiment #{event.experimentId}
                      </span>
                    )}
                  </div>
                  {event.description && <p className="mt-1 text-xs text-stone-700">{event.description}</p>}
                  {event.sourceReference && (
                    <p className="mt-1 text-[11px] text-stone-500">Ref: {event.sourceReference}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 4: Direction Changes */}
      {activeTab === 'changes' && (
        <div className="rounded-lg border border-stone-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-stone-950">Detected Research Direction Changes</h3>
          <p className="mt-1 text-xs text-stone-600">
            Supervision shifts flagged automatically by AI change detection or human pivots.
          </p>

          {summaryLoading ? (
            <p className="mt-4 text-xs text-stone-500">Loading direction shifts…</p>
          ) : changes.length === 0 ? (
            <p className="mt-4 text-xs text-stone-500">No research direction changes flagged.</p>
          ) : (
            <div className="mt-4 space-y-3">
              {changes.map((c) => (
                <div key={c.id} className="rounded-md border border-stone-200 bg-stone-50/50 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <ChangeStatusBadge status={c.status} />
                      <span className="text-xs font-bold text-stone-900">{c.title}</span>
                    </div>
                    <span className="text-[11px] text-stone-500">Detected: {c.detectedAt?.slice(0, 10)}</span>
                  </div>

                  <div className="mt-2 grid gap-2 sm:grid-cols-2 text-xs text-stone-700">
                    <div className="rounded border border-stone-200 bg-white p-2">
                      <span className="font-semibold text-stone-500 uppercase text-[10px]">Previous:</span>
                      <p className="mt-0.5">{c.previousApproach ?? '—'}</p>
                    </div>
                    <div className="rounded border border-indigo-200 bg-indigo-50/30 p-2">
                      <span className="font-semibold text-indigo-700 uppercase text-[10px]">New Pivot:</span>
                      <p className="mt-0.5">{c.newApproach ?? '—'}</p>
                    </div>
                  </div>

                  {c.reason && (
                    <p className="mt-2 text-xs text-stone-600">
                      <span className="font-medium text-stone-800">Reason:</span> {c.reason}
                    </p>
                  )}

                  {c.status === 'FLAGGED' && (
                    <div className="mt-3 flex gap-2 border-t border-stone-200 pt-2">
                      <button
                        type="button"
                        className="rounded bg-stone-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-stone-800"
                        onClick={() => void handleUpdateChangeStatus(c.id, 'REVIEWED')}
                      >
                        Confirm as Reviewed
                      </button>
                      <button
                        type="button"
                        className="rounded border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-700 hover:bg-stone-100"
                        onClick={() => void handleUpdateChangeStatus(c.id, 'DISMISSED')}
                      >
                        Dismiss Flag
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 5: Meeting Extraction & Human Review */}
      {activeTab === 'extraction' && (
        <div className="space-y-6">
          <div className="rounded-lg border border-stone-200 bg-white p-5">
            <h3 className="text-sm font-semibold text-stone-950">AI Research Extraction & Verification Gate</h3>
            <p className="mt-1 text-xs text-stone-600">
              Extract research candidates from supervision transcripts. Extracted items do not enter the research trace until approved by human review.
            </p>

            <div className="mt-4 flex flex-wrap items-end gap-3">
              <label className="block text-xs font-medium text-stone-700">
                Select Meeting Transcript
                <select
                  className="mt-1 block w-64 rounded-md border border-stone-300 bg-white px-3 py-2 text-xs text-stone-900"
                  value={meetingId ?? ''}
                  onChange={(e) => setMeetingId(e.target.value ? Number(e.target.value) : null)}
                  disabled={meetingsLoading}
                >
                  <option value="">Choose a meeting…</option>
                  {meetings.map((m, idx) => (
                    <option key={m.id ?? idx} value={m.id ?? ''}>
                      {m.title ?? 'Untitled'} ({m.meetingDate ?? 'No date'})
                    </option>
                  ))}
                </select>
                {meetingsError && (
                  <p className="mt-1 text-xs text-rose-600">{meetingsError}</p>
                )}
              </label>

              <button
                type="button"
                className="rounded-md bg-stone-950 px-4 py-2 text-xs font-medium text-white disabled:opacity-50"
                disabled={extracting || meetingId == null}
                onClick={() => void handleExtract()}
              >
                {extracting ? 'Extracting with Gemini…' : 'Extract Research with AI'}
              </button>
            </div>
          </div>

          {/* Current Extraction View with Review Gate */}
          {extraction && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between rounded-lg border border-stone-200 bg-stone-50 p-4">
                <div>
                  <h4 className="text-sm font-bold text-stone-950">Extracted Suggestions Pending Review</h4>
                  <p className="text-xs text-stone-600">
                    Accept to persist verified records into the research trace, or edit/reject.
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-md bg-emerald-700 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-800 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('ACCEPTED')}
                  >
                    {submittingReview === 'ACCEPTED' ? 'Saving…' : 'Accept All & Persist'}
                  </button>
                  <button
                    type="button"
                    className="rounded-md bg-indigo-700 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-800 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('EDITED')}
                  >
                    {submittingReview === 'EDITED' ? 'Saving…' : 'Save as Edited'}
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-stone-300 bg-white px-3 py-1.5 text-xs font-medium text-stone-700 hover:bg-stone-50 disabled:opacity-50"
                    disabled={submittingReview != null}
                    onClick={() => void handleReview('REJECTED')}
                  >
                    {submittingReview === 'REJECTED' ? 'Rejecting…' : 'Reject Suggestions'}
                  </button>
                </div>
              </div>

              {incompleteExtractionIssues(extraction).length > 0 ? (
                <ul className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-900" role="status">
                  {incompleteExtractionIssues(extraction).map((issue) => <li key={issue}>{issue}</li>)}
                </ul>
              ) : null}
              <ExtractionSections extraction={extraction} onChange={setExtraction} />
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
                      <span className="font-semibold text-stone-900">{r.status} · {extractionOrigin(r.originalExtractionJson, r.extractionJson)}</span>
                      <span className="text-stone-500">{r.createdAt?.replace('T', ' ').slice(0, 16)}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* Persisted Research Records Panel */}
      {activeProjectId != null ? <PersistedResearch
        projectId={activeProjectId}
        loading={summaryLoading}
        error={summaryError}
        decisions={decisions}
        experiments={experiments}
        results={results}
        events={timeline}
        onCreateExperiment={handleCreateExperiment}
        onLinkExperiment={handleLinkExperiment}
      /> : null}

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

              <label className="block text-xs font-medium text-stone-700">
                Related meeting (optional)
                <select
                  className="mt-1 block w-full rounded border border-stone-300 bg-white px-3 py-1.5 text-xs text-stone-900"
                  value={supersedeMeetingId ?? ''}
                  onChange={(e) => setSupersedeMeetingId(e.target.value ? Number(e.target.value) : null)}
                >
                  <option value="">No meeting</option>
                  {meetings
                    .filter((m) => m.id != null)
                    .map((m) => (
                      <option key={m.id} value={m.id ?? ''}>
                        {m.title ?? 'Untitled'} ({m.meetingDate ?? 'No date'})
                      </option>
                    ))}
                </select>
              </label>
            </div>

            {supersedeError && (
              <p className="mt-4 rounded border border-red-200 bg-red-50 p-3 text-xs font-medium text-red-800" role="alert">
                {supersedeError}
              </p>
            )}

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                className="rounded px-3 py-1.5 text-xs font-medium text-stone-600 hover:bg-stone-100"
                onClick={() => {
                  setSupersedeModalOpen(false)
                  setSupersedeError(null)
                }}
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
                <p className="mt-1 text-stone-600">
                  <span className="font-medium">Linked decision:</span> {lineage.decision.title ?? 'Untitled decision'}
                </p>

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
    TASK_COMPLETED: 'bg-emerald-100 text-emerald-800 border border-emerald-300',
  }
  return <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${map[type] ?? 'bg-stone-100 text-stone-800'}`}>{type}</span>
}

function decisionStatusClass(status: ResearchDecision['status']): string {
  return status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
}

function ChangeStatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = {
    FLAGGED: 'bg-amber-100 text-amber-800',
    REVIEWED: 'bg-emerald-100 text-emerald-800',
    DISMISSED: 'bg-stone-100 text-stone-600',
  }
  return <span className={`rounded px-2 py-0.5 text-[11px] font-semibold ${map[status] ?? 'bg-stone-100 text-stone-600'}`}>{status}</span>
}
