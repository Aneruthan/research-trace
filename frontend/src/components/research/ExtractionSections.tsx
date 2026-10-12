import type { ReactNode } from 'react'
import type { MeetingExtraction } from '../../api/research'

type ExtractionSectionsProps = {
  extraction: MeetingExtraction
}

export function ExtractionSections({ extraction }: ExtractionSectionsProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Section title="Tasks" empty="No tasks extracted.">
        {(extraction.tasks ?? []).map((item, index) => (
          <Item key={index} title={item.title} confidence={item.confidence} lines={lines(
            ['Description', item.description],
            ['Assignee id', item.assignee],
            ['Deadline', item.deadline],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Decisions" empty="No decisions extracted.">
        {(extraction.decisions ?? []).map((item, index) => (
          <Item key={index} title={item.title} confidence={item.confidence} lines={lines(
            ['Description', item.description],
            ['Made by', item.madeBy],
            ['Reason', item.reason],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Experiments" empty="No experiments extracted.">
        {(extraction.experiments ?? []).map((item, index) => (
          <Item key={index} title={item.title} confidence={item.confidence} lines={lines(
            ['Description', item.description],
            ['Objective', item.objective],
            ['Variables', item.variables],
            ['Expected outcome', item.expectedOutcome],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Results" empty="No results extracted.">
        {(extraction.results ?? []).map((item, index) => (
          <Item key={index} title={item.description} confidence={item.confidence} lines={lines(
            ['Experiment', item.experiment],
            ['Metrics', item.metrics],
            ['Comparison', item.comparison],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Deadlines" empty="No deadlines extracted.">
        {(extraction.deadlines ?? []).map((item, index) => (
          <Item key={index} title={item.description} confidence={item.confidence} lines={lines(
            ['Date', item.date],
            ['Relative date', item.relativeDate],
            ['Associated task', item.associatedTask],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Research entities" empty="No research entities extracted.">
        {(extraction.researchEntities ?? []).map((item, index) => (
          <Item key={index} title={item.name} confidence={item.confidence} lines={lines(
            ['Type', item.type],
            ['Description', item.description],
            ['Context', item.context],
            ['Source', item.source],
          )} />
        ))}
      </Section>
      <Section title="Research changes" empty="No research changes extracted.">
        {(extraction.researchChanges ?? []).map((item, index) => (
          <Item key={index} title={item.description} confidence={item.confidence} lines={lines(
            ['Previous approach', item.previousApproach],
            ['New approach', item.newApproach],
            ['Reason', item.reason],
            ['Source', item.source],
          )} />
        ))}
      </Section>
    </div>
  )
}

function Section({
  title,
  empty,
  children,
}: {
  title: string
  empty: string
  children: ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const visible = items.filter((item) => item != null)
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-stone-950">{title}</h3>
      {visible.length === 0 ? <p className="mt-2 text-sm text-stone-500">{empty}</p> : <ul className="mt-3 space-y-3">{children}</ul>}
    </section>
  )
}

function Item({
  title,
  confidence,
  lines: details,
}: {
  title: string | null
  confidence: number | null
  lines: string[]
}) {
  return (
    <li className="border-t border-stone-100 pt-3 first:border-t-0 first:pt-0">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-sm font-medium text-stone-900">{title ?? 'Untitled'}</p>
        <p className="text-xs text-stone-500">Confidence {confidence ?? '—'}</p>
      </div>
      {details.length > 0 ? (
        <ul className="mt-1 space-y-0.5 text-xs text-stone-600">
          {details.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      ) : null}
    </li>
  )
}

function lines(...pairs: Array<[string, string | number | null]>): string[] {
  return pairs
    .filter((pair) => pair[1] != null && String(pair[1]).trim() !== '')
    .map(([label, value]) => `${label}: ${value}`)
}
