import type { ReactNode } from 'react'
import type { Experiment, ResearchDecision, ResearchEvent, Result } from '../../api/research'

type PersistedResearchProps = {
  projectId: number
  loading: boolean
  error: string | null
  decisions: ResearchDecision[]
  experiments: Experiment[]
  results: Result[]
  events: ResearchEvent[]
}

export function PersistedResearch({
  projectId,
  loading,
  error,
  decisions,
  experiments,
  results,
  events,
}: PersistedResearchProps) {
  return (
    <section className="mt-8">
      <h2 className="text-sm font-semibold text-stone-950">Persisted research records</h2>
      <p className="mt-2 text-sm text-stone-600">
        These lists are stored records for project {projectId}. A review status is separate: only an accepted review
        creates them, and only for extraction fields the backend can store. Pending, rejected, and edited reviews stay
        in review history.
      </p>
      {loading ? <p className="mt-4 text-sm text-stone-600">Loading persisted research records…</p> : null}
      {!loading && error ? (
        <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error ? (
        <div className="mt-4 space-y-6">
          <RecordGroup
            title="Persisted research decisions"
            empty="No persisted decisions for this project. Save a review as ACCEPTED when the extraction includes a decision title."
          >
            {decisions.map((decision) => (
              <li key={decision.id ?? decision.title} className="px-4 py-4">
                <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                  <p className="text-sm font-medium text-stone-950">{decision.title ?? 'Untitled decision'}</p>
                  <p className="text-xs font-medium tracking-wide text-stone-500">{decision.status ?? 'No status'}</p>
                </div>
                <Field label="Text" value={decision.decisionText} />
                <Field label="Rationale" value={decision.rationale} />
                <Field label="Decided" value={formatTimestamp(decision.decidedAt)} />
                {decision.supersedesDecisionId != null ? (
                  <Field label="Supersedes" value={String(decision.supersedesDecisionId)} />
                ) : null}
                <Provenance
                  sourceReference={decision.sourceReference}
                  meetingId={decision.meetingId}
                  extractionReviewId={decision.extractionReviewId}
                />
              </li>
            ))}
          </RecordGroup>

          <RecordGroup
            title="Persisted experiments"
            empty="No persisted experiments for this project. An accepted review stores an experiment when it has a title."
          >
            {experiments.map((experiment) => (
              <li key={experiment.id ?? experiment.title} className="px-4 py-4">
                <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                  <p className="text-sm font-medium text-stone-950">{experiment.title ?? 'Untitled experiment'}</p>
                  <p className="text-xs font-medium tracking-wide text-stone-500">{experiment.status ?? 'No status'}</p>
                </div>
                <Field label="Hypothesis" value={experiment.hypothesis} />
                <Field label="Method" value={experiment.method} />
                <Field label="Started" value={formatTimestamp(experiment.startedAt)} />
                <Field label="Completed" value={formatTimestamp(experiment.completedAt)} />
                {experiment.researchDecisionId != null ? (
                  <Field label="Decision" value={String(experiment.researchDecisionId)} />
                ) : null}
                <Provenance sourceReference={experiment.sourceReference} />
              </li>
            ))}
          </RecordGroup>

          <RecordGroup
            title="Persisted results"
            empty="No persisted results for this project. An accepted review stores a result when it has a summary and a matching experiment."
          >
            {results.map((result) => (
              <li key={result.id ?? result.summary} className="px-4 py-4">
                <p className="text-sm font-medium text-stone-950">{result.summary ?? 'No summary'}</p>
                <Field label="Metrics" value={result.dataOrMetric} />
                <Field label="Observed" value={formatTimestamp(result.observedAt)} />
                {result.experimentId != null ? (
                  <Field label="Experiment" value={String(result.experimentId)} />
                ) : null}
                <Provenance sourceReference={result.sourceReference} />
              </li>
            ))}
          </RecordGroup>

          <RecordGroup
            title="Research-event timeline"
            empty="No research events for this project. The backend writes an event when it stores a decision, experiment, or result. This page does not create events from extraction JSON."
          >
            {events.map((event) => (
              <li key={event.id ?? `${event.eventType}-${event.occurredAt}`} className="px-4 py-4">
                <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                  <p className="text-sm font-medium text-stone-950">{event.title ?? 'Untitled event'}</p>
                  <p className="text-xs font-medium tracking-wide text-stone-500">{event.eventType ?? 'No type'}</p>
                </div>
                <Field label="Description" value={event.description} />
                <Field label="Occurred" value={formatTimestamp(event.occurredAt)} />
                <Field label="Source" value={event.sourceReference} />
                {event.taskId != null && <Field label="Task" value={`#${event.taskId}`} />}
                {event.researchDecisionId != null && <Field label="Decision" value={`#${event.researchDecisionId}`} />}
                {event.experimentId != null && <Field label="Experiment" value={`#${event.experimentId}`} />}
              </li>
            ))}
          </RecordGroup>
        </div>
      ) : null}
    </section>
  )
}

function RecordGroup({
  title,
  empty,
  children,
}: {
  title: string
  empty: string
  children: ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const visible = items.filter((item) => item != null && item !== false)
  return (
    <section>
      <h3 className="text-sm font-semibold text-stone-950">{title}</h3>
      {visible.length === 0 ? (
        <p className="mt-2 text-sm text-stone-500">{empty}</p>
      ) : (
        <ul className="mt-3 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {children}
        </ul>
      )}
    </section>
  )
}

function Field({ label, value }: { label: string; value: string | null }) {
  const text = value == null || value.trim() === '' ? '—' : value
  return (
    <p className="mt-1 text-xs text-stone-600">
      {label}: {text}
    </p>
  )
}

function Provenance({
  sourceReference,
  meetingId,
  extractionReviewId,
}: {
  sourceReference: string | null
  meetingId?: number | null
  extractionReviewId?: number | null
}) {
  return (
    <>
      {sourceReference != null && sourceReference.trim() !== '' ? (
        <Field label="Source" value={sourceReference} />
      ) : null}
      {meetingId != null ? <Field label="Meeting" value={String(meetingId)} /> : null}
      {extractionReviewId != null ? <Field label="Review" value={String(extractionReviewId)} /> : null}
    </>
  )
}

function formatTimestamp(value: string | null): string | null {
  if (value == null || value.trim() === '') {
    return null
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return parsed.toLocaleString()
}
