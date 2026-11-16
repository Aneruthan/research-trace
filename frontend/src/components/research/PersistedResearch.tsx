import { useState, type FormEvent, type ReactNode } from 'react'
import type { Experiment, ResearchDecision, ResearchEvent, Result } from '../../api/research'
import { formatDateOrTimestamp } from '../common/dates'

type PersistedResearchProps = {
  projectId: number
  loading: boolean
  error: string | null
  decisions: ResearchDecision[]
  experiments: Experiment[]
  results: Result[]
  events: ResearchEvent[]
  onCreateExperiment?: (title: string, decisionId: number | null) => Promise<void>
  onLinkExperiment?: (experimentId: number, decisionId: number | null) => Promise<void>
}

export function PersistedResearch({
  projectId,
  loading,
  error,
  decisions,
  experiments,
  results,
  events,
  onCreateExperiment,
  onLinkExperiment,
}: PersistedResearchProps) {
  return (
    <section className="mt-8">
      <h2 className="text-sm font-semibold text-stone-950">Persisted research records</h2>
      <p className="mt-2 text-sm text-stone-600">
        These lists are stored records for project {projectId}. A review status is separate: only an accepted review
        creates them, and only for extraction fields the backend can store. Pending, rejected, and edited reviews add
        no decisions, experiments, or results; an edited review can still flag direction changes.
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
                <Field label="Decided" value={formatDateOrTimestamp(decision.decidedAt)} />
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

          {onCreateExperiment ? <CreateExperimentForm decisions={decisions} onCreate={onCreateExperiment} /> : null}
          <RecordGroup
            title="Persisted experiments"
            empty="No persisted experiments for this project. An accepted review stores an experiment when it has a title. Linking a decision is optional and is never guessed from the meeting notes."
          >
            {experiments.map((experiment) => (
              <li key={experiment.id ?? experiment.title} className="px-4 py-4">
                <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                  <p className="text-sm font-medium text-stone-950">{experiment.title ?? 'Untitled experiment'}</p>
                  <p className="text-xs font-medium tracking-wide text-stone-500">{experiment.status ?? 'No status'}</p>
                </div>
                <Field label="Hypothesis" value={experiment.hypothesis} />
                <Field label="Method" value={experiment.method} />
                <Field label="Started" value={formatDateOrTimestamp(experiment.startedAt)} />
                <Field label="Completed" value={formatDateOrTimestamp(experiment.completedAt)} />
                <Field label="Decision" value={decisionTitle(decisions, experiment.researchDecisionId)} />
                {onLinkExperiment && experiment.id != null ? (
                  <ExperimentDecisionEditor
                    experimentId={experiment.id}
                    decisionId={experiment.researchDecisionId}
                    decisions={decisions}
                    onLink={onLinkExperiment}
                  />
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
                <Field label="Observed" value={formatDateOrTimestamp(result.observedAt)} />
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
                <Field label="Occurred" value={formatDateOrTimestamp(event.occurredAt)} />
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

function decisionTitle(decisions: ResearchDecision[], decisionId: number | null): string | null {
  if (decisionId == null) {
    return null
  }
  return decisions.find((decision) => decision.id === decisionId)?.title ?? 'Unknown decision'
}

function CreateExperimentForm({
  decisions,
  onCreate,
}: {
  decisions: ResearchDecision[]
  onCreate: (title: string, decisionId: number | null) => Promise<void>
}) {
  const [title, setTitle] = useState('')
  const [decisionId, setDecisionId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (title.trim() === '') {
      setError('Experiment title is required')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onCreate(title.trim(), decisionId === '' ? null : Number(decisionId))
      setTitle('')
      setDecisionId('')
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not create experiment')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="rounded-lg border border-stone-200 bg-white p-4" onSubmit={(event) => void submit(event)}>
      <h3 className="text-sm font-semibold text-stone-950">Add experiment</h3>
      <p className="mt-1 text-xs text-stone-600">
        The decision link is optional. Existing experiments stay unlinked until you choose one.
      </p>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <label className="block text-xs font-medium text-stone-600">
          Title
          <input
            className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <DecisionSelect decisions={decisions} value={decisionId} onChange={setDecisionId} />
      </div>
      {error ? <p className="mt-2 text-xs text-red-800">{error}</p> : null}
      <button
        type="submit"
        className="mt-3 rounded-md bg-stone-950 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
        disabled={saving}
      >
        {saving ? 'Saving…' : 'Add experiment'}
      </button>
    </form>
  )
}

function ExperimentDecisionEditor({
  experimentId,
  decisionId,
  decisions,
  onLink,
}: {
  experimentId: number
  decisionId: number | null
  decisions: ResearchDecision[]
  onLink: (experimentId: number, decisionId: number | null) => Promise<void>
}) {
  const [selected, setSelected] = useState(decisionId?.toString() ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function save() {
    setSaving(true)
    setError(null)
    try {
      await onLink(experimentId, selected === '' ? null : Number(selected))
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update the decision link')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mt-2">
      <div className="flex flex-wrap items-end gap-2">
        <DecisionSelect decisions={decisions} value={selected} onChange={setSelected} />
        <button
          type="button"
          className="rounded-md border border-stone-300 px-3 py-2 text-xs font-medium text-stone-800 disabled:opacity-60"
          onClick={() => void save()}
          disabled={saving}
        >
          {saving ? 'Saving…' : 'Save decision link'}
        </button>
      </div>
      {error ? <p className="mt-1 text-xs text-red-800">{error}</p> : null}
    </div>
  )
}

function DecisionSelect({
  decisions,
  value,
  onChange,
}: {
  decisions: ResearchDecision[]
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label className="block text-xs font-medium text-stone-600">
      Linked decision (optional)
      <select
        className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value="">No linked decision</option>
        {decisions
          .filter((decision) => decision.id != null)
          .map((decision) => (
            <option key={decision.id} value={decision.id ?? ''}>
              {decision.title ?? 'Untitled decision'}
            </option>
          ))}
      </select>
    </label>
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
