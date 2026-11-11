import { useEffect, useState } from 'react'
import { getMilestoneProgress, getProjectMilestones, updateMilestone } from '../api/milestones'
import type { Milestone, MilestoneStatus } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

type MilestoneRow = {
  milestone: Milestone
  progress: number | null
}

const MILESTONE_STATUSES: MilestoneStatus[] = [
  'NOT_STARTED',
  'IN_PROGRESS',
  'COMPLETED',
  'BLOCKED',
  'DEFERRED',
]

export function Milestones() {
  const [rows, setRows] = useState<MilestoneRow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Milestone | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    function load() {
      getProjectMilestones(SELECTED_PROJECT_ID, controller.signal)
        .then(async (milestones) => {
          const loaded = await Promise.all(milestones.map(async (milestone) => {
            if (milestone.id == null) {
              return { milestone, progress: null }
            }
            try {
              const progress = await getMilestoneProgress(milestone.id, SELECTED_PROJECT_ID, controller.signal)
              return { milestone, progress }
            } catch {
              return { milestone, progress: null }
            }
          }))
          if (active && !controller.signal.aborted) {
            setRows(loaded)
            setError(null)
          }
        })
        .catch((cause: unknown) => {
          if (!active || controller.signal.aborted) {
            return
          }
          setError(cause instanceof Error ? cause.message : 'Could not load milestones')
        })
        .finally(() => {
          if (active && !controller.signal.aborted) {
            setLoading(false)
          }
        })
    }

    setLoading(true)
    setError(null)
    load()
    window.addEventListener('researchtrace-progress-changed', load)
    return () => {
      active = false
      controller.abort()
      window.removeEventListener('researchtrace-progress-changed', load)
    }
  }, [])

  async function saveMilestone() {
    if (editing == null || editing.id == null) {
      return
    }
    const title = editing.title?.trim() ?? ''
    if (title.length === 0) {
      setError('Milestone title cannot be empty')
      setMessage(null)
      return
    }
    if (editing.weight != null && editing.weight < 0) {
      setError('Milestone weight cannot be negative')
      setMessage(null)
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await updateMilestone({ ...editing, title })
      setMessage(`Saved milestone “${title}”.`)
      setEditing(null)
      window.dispatchEvent(new Event('researchtrace-progress-changed'))
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update milestone')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section>
      <PageHeader
        title="Milestones"
        description="Milestone weights and progress derived from completed tasks."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading milestones for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && message ? (
        <p className="mt-6 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800">
          {message}
        </p>
      ) : null}
      {!loading && error ? (
        <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && rows.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No milestones for this project.</p>
      ) : null}
      {!loading && rows.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {rows.map(({ milestone, progress }) => (
            <li key={milestone.id ?? milestone.title} className="px-4 py-4">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
                <p className="text-sm font-medium text-stone-950">{milestone.title ?? 'Untitled milestone'}</p>
                <div className="flex items-center gap-3">
                  <p className="text-xs font-medium tracking-wide text-stone-500">
                    {milestone.status ?? 'No status'}
                  </p>
                  {milestone.id != null ? (
                    <button
                      type="button"
                      className="rounded-md border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-800"
                      onClick={() => {
                        setEditing({ ...milestone })
                        setError(null)
                        setMessage(null)
                      }}
                    >
                      Edit
                    </button>
                  ) : null}
                </div>
              </div>
              <p className="mt-1 text-xs text-stone-500">
                Weight {milestone.weight ?? 'none'}
                {' · '}
                Progress {progress == null ? 'unavailable' : `${progress.toFixed(2)}%`}
                {milestone.dueDate ? ` · Due ${milestone.dueDate}` : ''}
              </p>
              {editing?.id === milestone.id ? (
                <form
                  className="mt-3 grid gap-3 sm:grid-cols-2"
                  onSubmit={(event) => {
                    event.preventDefault()
                    void saveMilestone()
                  }}
                >
                  <label className="block text-xs font-medium text-stone-600">
                    Title
                    <input
                      className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
                      value={editing.title ?? ''}
                      onChange={(event) => setEditing({ ...editing, title: event.target.value })}
                    />
                  </label>
                  <label className="block text-xs font-medium text-stone-600">
                    Weight
                    <input
                      className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
                      type="number"
                      min="0"
                      step="0.01"
                      value={editing.weight ?? ''}
                      onChange={(event) => setEditing({
                        ...editing,
                        weight: event.target.value === '' ? null : Number(event.target.value),
                      })}
                    />
                  </label>
                  <label className="block text-xs font-medium text-stone-600">
                    Status
                    <select
                      className="mt-1 block w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm text-stone-900"
                      value={editing.status ?? 'NOT_STARTED'}
                      onChange={(event) => setEditing({
                        ...editing,
                        status: event.target.value as MilestoneStatus,
                      })}
                    >
                      {MILESTONE_STATUSES.map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  </label>
                  <label className="block text-xs font-medium text-stone-600">
                    Due date
                    <input
                      className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
                      type="date"
                      value={editing.dueDate ?? ''}
                      onChange={(event) => setEditing({
                        ...editing,
                        dueDate: event.target.value === '' ? null : event.target.value,
                      })}
                    />
                  </label>
                  <label className="block text-xs font-medium text-stone-600 sm:col-span-2">
                    Description
                    <input
                      className="mt-1 block w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
                      value={editing.description ?? ''}
                      onChange={(event) => setEditing({ ...editing, description: event.target.value })}
                    />
                  </label>
                  <div className="flex gap-2 sm:col-span-2">
                    <button
                      type="submit"
                      className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60"
                      disabled={saving}
                    >
                      {saving ? 'Saving…' : 'Save milestone'}
                    </button>
                    <button
                      type="button"
                      className="rounded-md border border-stone-300 bg-white px-3 py-2 text-sm font-medium text-stone-800"
                      onClick={() => setEditing(null)}
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}
