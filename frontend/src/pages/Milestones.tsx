import { useEffect, useRef, useState } from 'react'
import { createMilestone, getMilestoneProgress, getProjectMilestones, updateMilestone } from '../api/milestones'
import { getProjectTasks } from '../api/tasks'
import type { Milestone, MilestoneStatus, Task } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { validateMilestoneDraft } from '../work/work'

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
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [rows, setRows] = useState<MilestoneRow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Milestone | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [creating, setCreating] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')
  const [newDueDate, setNewDueDate] = useState('')
  const [newStatus, setNewStatus] = useState<MilestoneStatus>('NOT_STARTED')
  const [newWeight, setNewWeight] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    function load() {
      if (activeProjectId == null) {
        return
      }
      const projectId = activeProjectId
      Promise.all([
        getProjectMilestones(projectId, controller.signal),
        getProjectTasks(projectId, controller.signal).catch(() => [] as Task[]),
      ])
        .then(async ([milestones, loadedTasks]) => {
          const loaded = await Promise.all(milestones.map(async (milestone) => {
            if (milestone.id == null) {
              return { milestone, progress: null }
            }
            try {
              const progress = await getMilestoneProgress(projectId, milestone.id, controller.signal)
              return { milestone, progress }
            } catch {
              return { milestone, progress: null }
            }
          }))
          if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            setRows(loaded)
            setTasks(loadedTasks)
            setError(null)
          }
        })
        .catch((cause: unknown) => {
          if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            return
          }
          setError(cause instanceof Error ? cause.message : 'Could not load milestones')
        })
        .finally(() => {
          if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            setLoading(false)
          }
        })
    }

    setRows([])
    setTasks([])
    setError(null)
    if (projectsLoading) {
      setLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null) {
      setLoading(false)
      return () => controller.abort()
    }
    setLoading(true)
    load()
    window.addEventListener('researchtrace-progress-changed', load)
    return () => {
      controller.abort()
      window.removeEventListener('researchtrace-progress-changed', load)
    }
  }, [activeProjectId, projectsLoading, projectsError])

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
        description="Progress is calculated from tasks. Milestone weight is kept for reference and does not change progress."
      />
      {projectsLoading || (loading && activeProjectId != null) ? (
        <p className="mt-6 text-sm text-stone-600">Loading milestones for the selected project…</p>
      ) : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading ? (
        <form className="mt-6 grid gap-3 rounded-lg border border-stone-200 bg-white p-4 sm:grid-cols-2" onSubmit={(event) => {
          event.preventDefault()
          const validation = validateMilestoneDraft(newTitle, newWeight)
          if (validation || activeProjectId == null) {
            setError(validation)
            return
          }
          setSaving(true)
          setCreating(true)
          void createMilestone(activeProjectId, {
            title: newTitle.trim(),
            description: newDescription.trim() || null,
            status: newStatus,
            dueDate: newDueDate || null,
            weight: newWeight.trim() === '' ? null : Number(newWeight),
          }).then(() => {
            setNewTitle('')
            setNewDescription('')
            setNewDueDate('')
            setNewWeight('')
            setMessage('Milestone created.')
            window.dispatchEvent(new Event('researchtrace-progress-changed'))
          }).catch((cause: unknown) => {
            setError(cause instanceof Error ? cause.message : 'Could not create milestone')
          }).finally(() => {
            setSaving(false)
            setCreating(false)
          })
        }}>
          <h2 className="text-sm font-semibold text-stone-950 sm:col-span-2">Create milestone</h2>
          <label className="text-xs font-medium text-stone-600">Title
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={newTitle} onChange={(event) => setNewTitle(event.target.value)} />
          </label>
          <label className="text-xs font-medium text-stone-600">Description
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={newDescription} onChange={(event) => setNewDescription(event.target.value)} />
          </label>
          <label className="text-xs font-medium text-stone-600">Due date
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" type="date" value={newDueDate} onChange={(event) => setNewDueDate(event.target.value)} />
          </label>
          <label className="text-xs font-medium text-stone-600">Status
            <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={newStatus} onChange={(event) => setNewStatus(event.target.value as MilestoneStatus)}>
              {MILESTONE_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
            </select>
          </label>
          <label className="text-xs font-medium text-stone-600">Weight
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" type="number" min="0" step="0.01" value={newWeight} onChange={(event) => setNewWeight(event.target.value)} />
          </label>
          <button className="w-fit rounded-md bg-stone-950 px-3 py-2 text-sm text-white disabled:opacity-60" disabled={saving || creating} type="submit">Create milestone</button>
        </form>
      ) : null}
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
      {!projectsLoading && activeProjectId != null && !loading && !error && rows.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No milestones for this project.</p>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && !error && rows.length > 0 ? (
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
                Weight {milestone.weight ?? 'none'} (not used in progress)
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
                    Weight (not used in progress)
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
              <ul className="mt-2 space-y-1 text-xs text-stone-600">
                {tasks.filter((task) => task.milestoneId === milestone.id).map((task) => (
                  <li key={task.id}>{task.title} · {task.status}</li>
                ))}
                {tasks.filter((task) => task.milestoneId === milestone.id).length === 0 ? <li>No tasks for this milestone.</li> : null}
              </ul>
            </li>
          ))}
        </ul>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading ? (
        <section className="mt-6">
          <h2 className="text-sm font-semibold text-stone-950">Tasks without a milestone</h2>
          <ul className="mt-2 space-y-1 text-sm text-stone-600">
            {tasks.filter((task) => task.milestoneId == null).map((task) => <li key={task.id}>{task.title}</li>)}
            {tasks.filter((task) => task.milestoneId == null).length === 0 ? <li>None</li> : null}
          </ul>
        </section>
      ) : null}
    </section>
  )
}
