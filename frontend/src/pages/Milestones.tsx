import { useEffect, useState } from 'react'
import { getMilestoneProgress, getProjectMilestones } from '../api/milestones'
import type { Milestone } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

type MilestoneRow = {
  milestone: Milestone
  progress: number | null
}

export function Milestones() {
  const [rows, setRows] = useState<MilestoneRow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)

    getProjectMilestones(SELECTED_PROJECT_ID, controller.signal)
      .then(async (milestones) => {
        const loaded = await Promise.all(milestones.map(async (milestone) => {
          if (milestone.id == null) {
            return { milestone, progress: null }
          }
          try {
            const progress = await getMilestoneProgress(milestone.id, controller.signal)
            return { milestone, progress }
          } catch {
            return { milestone, progress: null }
          }
        }))
        if (!controller.signal.aborted) {
          setRows(loaded)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setError(cause instanceof Error ? cause.message : 'Could not load milestones')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [])

  return (
    <section>
      <PageHeader
        title="Milestones"
        description="Milestone weights and progress derived from completed tasks."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading milestones for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && error ? (
        <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && rows.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No milestones for this project.</p>
      ) : null}
      {!loading && !error && rows.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {rows.map(({ milestone, progress }) => (
            <li key={milestone.id ?? milestone.title} className="px-4 py-4">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                <p className="text-sm font-medium text-stone-950">{milestone.title ?? 'Untitled milestone'}</p>
                <p className="text-xs font-medium tracking-wide text-stone-500">
                  {milestone.status ?? 'No status'}
                </p>
              </div>
              <p className="mt-1 text-xs text-stone-500">
                Weight {milestone.weight ?? 'none'}
                {' · '}
                Progress {progress == null ? 'unavailable' : `${progress.toFixed(2)}%`}
                {milestone.dueDate ? ` · Due ${milestone.dueDate}` : ''}
              </p>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}
