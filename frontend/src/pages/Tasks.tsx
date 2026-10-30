import { useEffect, useState } from 'react'
import { getProjectProgress, getProjectTasks, updateTaskStatus } from '../api/tasks'
import type { Task, TaskStatus, User } from '../api/types'
import { assigneeLabel, getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

export function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [projectProgress, setProjectProgress] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)

  async function load(signal?: AbortSignal) {
    const [loadedTasks, loadedUsers, progress] = await Promise.all([
      getProjectTasks(SELECTED_PROJECT_ID, signal),
      getUsers(signal).catch(() => [] as User[]),
      getProjectProgress(SELECTED_PROJECT_ID, signal).catch(() => null),
    ])
    if (signal?.aborted) {
      return
    }
    setTasks(loadedTasks)
    setUsers(loadedUsers)
    setProjectProgress(progress)
  }

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)
    load(controller.signal)
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setError(cause instanceof Error ? cause.message : 'Could not load tasks')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [])

  async function changeStatus(task: Task, status: TaskStatus) {
    if (task.id == null || task.status === status) {
      return
    }
    setSavingId(task.id)
    setError(null)
    try {
      await updateTaskStatus(task, status)
      await load()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update task')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <section>
      <PageHeader
        title="Tasks"
        description="Accepted tasks, student assignment, and status updates."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading tasks for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && projectProgress != null ? (
        <p className="mt-4 text-sm text-stone-700">
          Project progress from the backend: {projectProgress.toFixed(2)}%
        </p>
      ) : null}
      {!loading && error ? <Alert message={error} /> : null}
      {!loading && !error && tasks.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No tasks for this project.</p>
      ) : null}
      {!loading && !error && tasks.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {tasks.map((task) => (
            <li
              key={task.id ?? task.title}
              className="flex flex-col gap-1 px-4 py-4 sm:flex-row sm:items-baseline sm:justify-between"
            >
              <div>
                <p className="text-sm font-medium text-stone-950">{task.title ?? 'Untitled task'}</p>
                <p className="text-xs text-stone-500">
                  Assignee {assigneeLabel(task.assigneeId, users)}
                  {' · '}
                  Milestone {task.milestoneId ?? 'none'}
                  {' · '}
                  {task.dueDate ? `Due ${task.dueDate}` : 'No due date'}
                </p>
              </div>
              <span className="text-xs font-medium tracking-wide text-stone-500">
                <label className="sr-only" htmlFor={`status-${task.id}`}>Status</label>
                <select
                  id={`status-${task.id}`}
                  className="rounded-md border border-stone-300 bg-white px-2 py-1 text-xs text-stone-800"
                  value={task.status ?? 'TODO'}
                  disabled={task.id == null || savingId === task.id}
                  onChange={(event) => void changeStatus(task, event.target.value as TaskStatus)}
                >
                  <option value="TODO">TODO</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="COMPLETED">COMPLETED</option>
                </select>
              </span>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}

function Alert({ message }: { message: string }) {
  return (
    <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      {message}
    </p>
  )
}
