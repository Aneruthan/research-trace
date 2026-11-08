import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjectMeetings } from '../api/meetings'
import { getProjectMilestones } from '../api/milestones'
import {
  getProjectExperiments,
  getProjectResearchDecisions,
  type Experiment,
  type ResearchDecision,
} from '../api/research'
import { getProjectProgress, getProjectTasks, updateTaskStatus } from '../api/tasks'
import type { Meeting, Milestone, Task, TaskStatus, User } from '../api/types'
import { assigneeLabel, getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'

const SELECTED_PROJECT_ID = 1

export function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [milestones, setMilestones] = useState<Milestone[]>([])
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [decisions, setDecisions] = useState<ResearchDecision[]>([])
  const [experiments, setExperiments] = useState<Experiment[]>([])
  const [projectProgress, setProjectProgress] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)

  async function load(signal?: AbortSignal) {
    const [
      loadedTasks,
      loadedUsers,
      progress,
      loadedMilestones,
      loadedMeetings,
      loadedDecisions,
      loadedExperiments,
    ] = await Promise.all([
      getProjectTasks(SELECTED_PROJECT_ID, signal),
      getUsers(signal).catch(() => [] as User[]),
      getProjectProgress(SELECTED_PROJECT_ID, signal).catch(() => null),
      getProjectMilestones(SELECTED_PROJECT_ID, signal).catch(() => [] as Milestone[]),
      getProjectMeetings(SELECTED_PROJECT_ID, signal).catch(() => [] as Meeting[]),
      getProjectResearchDecisions(SELECTED_PROJECT_ID, signal).catch(() => [] as ResearchDecision[]),
      getProjectExperiments(SELECTED_PROJECT_ID, signal).catch(() => [] as Experiment[]),
    ])
    if (signal?.aborted) {
      return
    }
    setTasks(loadedTasks)
    setUsers(loadedUsers)
    setProjectProgress(progress)
    setMilestones(loadedMilestones)
    setMeetings(loadedMeetings)
    setDecisions(loadedDecisions)
    setExperiments(loadedExperiments)
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
    const targetTaskId = task.id
    setSavingId(targetTaskId)
    setError(null)
    try {
      const updated = await updateTaskStatus(task, status)
      // Immediately reflect updated task status in state to guarantee responsive UI
      setTasks((prev) =>
        prev.map((t) => (t.id === targetTaskId ? { ...t, status: updated.status ?? status } : t)),
      )
      // Refresh authoritative backend progress and aggregates
      await load()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update task')
      // On failure, reload server state to ensure UI remains consistent with persisted database
      await load().catch(() => {})
    } finally {
      setSavingId(null)
    }
  }

  const milestonesMap = new Map<number, string>(
    milestones
      .filter((m) => m.id != null && m.title != null)
      .map((m) => [m.id as number, m.title as string]),
  )

  const meetingsMap = new Map<number, string>(
    meetings
      .filter((m) => m.id != null && m.title != null)
      .map((m) => [m.id as number, m.title as string]),
  )

  const decisionsMap = new Map<number, string>(
    decisions
      .filter((d) => d.id != null && d.title != null)
      .map((d) => [d.id as number, d.title as string]),
  )

  const experimentsMap = new Map<number, string>(
    experiments
      .filter((e) => e.id != null && e.title != null)
      .map((e) => [e.id as number, e.title as string]),
  )

  const validTasks = tasks.filter((t): t is Task & { id: number } => t.id != null)

  return (
    <section>
      <PageHeader
        title="Tasks"
        description="Accepted tasks, student assignment, research motivation, and status updates."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading tasks for project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && projectProgress != null ? (
        <p className="mt-4 text-sm text-stone-700" data-testid="project-progress-label">
          Project progress from the backend: {projectProgress.toFixed(2)}%
        </p>
      ) : null}
      {!loading && error ? <Alert message={error} /> : null}
      {!loading && !error && validTasks.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No tasks for this project.</p>
      ) : null}
      {!loading && !error && validTasks.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {validTasks.map((task) => {
            const milestoneName = task.milestoneId != null
              ? milestonesMap.get(task.milestoneId) ?? `Milestone #${task.milestoneId}`
              : 'No milestone'

            const meetingTitle = task.meetingId != null
              ? meetingsMap.get(task.meetingId) ?? `Meeting #${task.meetingId}`
              : null

            const decisionTitle = task.researchDecisionId != null
              ? decisionsMap.get(task.researchDecisionId) ?? `Decision #${task.researchDecisionId}`
              : null

            const experimentTitle = task.experimentId != null
              ? experimentsMap.get(task.experimentId) ?? `Experiment #${task.experimentId}`
              : null

            return (
              <li
                key={task.id}
                id={`task-item-${task.id}`}
                className="flex flex-col gap-2 px-4 py-4 sm:flex-row sm:items-start sm:justify-between"
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium text-stone-950">{task.title ?? 'Untitled task'}</p>
                  {task.description ? (
                    <p className="text-xs text-stone-600">{task.description}</p>
                  ) : null}
                  <p className="text-xs text-stone-500">
                    Assignee: {assigneeLabel(task.assigneeId, users)}
                    {' · '}
                    Milestone: {milestoneName}
                    {' · '}
                    {task.dueDate ? `Due ${task.dueDate}` : 'No due date'}
                  </p>
                  <div className="flex flex-wrap items-center gap-2 pt-1">
                    {meetingTitle ? (
                      <Link
                        to={`/meetings/${task.meetingId}`}
                        className="inline-flex items-center rounded border border-stone-200 bg-stone-50 px-2 py-0.5 text-[11px] font-medium text-stone-700 hover:bg-stone-100"
                        title="View originating meeting"
                      >
                        Meeting: {meetingTitle}
                      </Link>
                    ) : null}
                    {decisionTitle ? (
                      <Link
                        to="/research-trace"
                        className="inline-flex items-center rounded border border-stone-200 bg-stone-50 px-2 py-0.5 text-[11px] font-medium text-stone-700 hover:bg-stone-100"
                        title="Motivated by research decision"
                      >
                        Decision: {decisionTitle}
                      </Link>
                    ) : null}
                    {experimentTitle ? (
                      <Link
                        to="/research-trace"
                        className="inline-flex items-center rounded border border-stone-200 bg-stone-50 px-2 py-0.5 text-[11px] font-medium text-stone-700 hover:bg-stone-100"
                        title="Linked to experiment"
                      >
                        Experiment: {experimentTitle}
                      </Link>
                    ) : null}
                  </div>
                </div>
                <span className="text-xs font-medium tracking-wide text-stone-500 sm:self-center">
                  <label className="sr-only" htmlFor={`status-${task.id}`}>Status</label>
                  <select
                    id={`status-${task.id}`}
                    data-testid={`status-select-${task.id}`}
                    className="rounded-md border border-stone-300 bg-white px-2 py-1 text-xs text-stone-800"
                    value={task.status ?? 'TODO'}
                    disabled={savingId === task.id}
                    onChange={(event) => void changeStatus(task, event.target.value as TaskStatus)}
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="COMPLETED">COMPLETED</option>
                  </select>
                </span>
              </li>
            )
          })}
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
