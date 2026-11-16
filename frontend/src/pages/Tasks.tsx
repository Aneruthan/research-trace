import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjectMeetings } from '../api/meetings'
import { getProjectMilestones } from '../api/milestones'
import {
  getProjectExperiments,
  getProjectResearchDecisions,
  type Experiment,
  type ResearchDecision,
} from '../api/research'
import { createProjectTask, deleteProjectTask, getProjectProgress, getProjectTasks, updateProjectTask, updateTaskStatus } from '../api/tasks'
import type { Meeting, Milestone, ProjectMember, Task, TaskStatus, User } from '../api/types'
import { assigneeLabel, getProjectMembers, getUsers } from '../api/users'
import { ConfirmDialog } from '../components/common/ConfirmDialog'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { localIsoDate } from '../components/common/dates'
import { filterTasks, isOverdueTask, studentFilterOptions, validateTaskDraft, type TaskListFilter } from '../work/work'

export function Tasks() {
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [tasks, setTasks] = useState<Task[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [usersUnavailable, setUsersUnavailable] = useState(false)
  const [milestones, setMilestones] = useState<Milestone[]>([])
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [decisions, setDecisions] = useState<ResearchDecision[]>([])
  const [experiments, setExperiments] = useState<Experiment[]>([])
  const [projectProgress, setProjectProgress] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [members, setMembers] = useState<ProjectMember[]>([])
  const [filter, setFilter] = useState<TaskListFilter>('ALL')
  const [studentFilter, setStudentFilter] = useState('')
  const [milestoneFilter, setMilestoneFilter] = useState('')
  const [draft, setDraft] = useState<Task | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Task | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function load(projectId: number, signal?: AbortSignal) {
    const [
      loadedTasks,
      loadedUsers,
      progress,
      loadedMilestones,
      loadedMeetings,
      loadedDecisions,
      loadedExperiments,
      loadedMembers,
    ] = await Promise.all([
      getProjectTasks(projectId, signal),
      getUsers(signal).catch(() => null),
      getProjectProgress(projectId, signal).catch(() => null),
      getProjectMilestones(projectId, signal).catch(() => [] as Milestone[]),
      getProjectMeetings(projectId, signal).catch(() => [] as Meeting[]),
      getProjectResearchDecisions(projectId, signal).catch(() => [] as ResearchDecision[]),
      getProjectExperiments(projectId, signal).catch(() => [] as Experiment[]),
      getProjectMembers(projectId, signal).catch(() => [] as ProjectMember[]),
    ])
    if (signal?.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
      return
    }
    setTasks(loadedTasks)
    setUsers(loadedUsers ?? [])
    setUsersUnavailable(loadedUsers == null)
    setProjectProgress(progress)
    setMilestones(loadedMilestones)
    setMeetings(loadedMeetings)
    setDecisions(loadedDecisions)
    setExperiments(loadedExperiments)
    setMembers(loadedMembers)
  }

  useEffect(() => {
    const controller = new AbortController()
    setTasks([])
    setProjectProgress(null)
    setError(null)
    setStudentFilter('')
    setMilestoneFilter('')
    setMessage(null)
    if (projectsLoading) {
      setLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null) {
      setLoading(false)
      return () => controller.abort()
    }
    const projectId = activeProjectId
    setLoading(true)
    load(projectId, controller.signal)
      .catch((cause: unknown) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          return
        }
        setError(cause instanceof Error ? cause.message : 'Could not load tasks')
      })
      .finally(() => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [activeProjectId, projectsLoading, projectsError])

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
      if (activeProjectId != null) {
        await load(activeProjectId)
      }
      window.dispatchEvent(new Event('researchtrace-progress-changed'))
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update task')
      if (activeProjectId != null) {
        await load(activeProjectId).catch(() => {})
      }
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
  const today = localIsoDate()
  const milestoneSelection = milestoneFilter === 'none'
    ? 'none'
    : milestoneFilter === '' ? null : Number(milestoneFilter)
  const visibleTasks = filterTasks(
    validTasks,
    filter,
    today,
    studentFilter === '' ? null : Number(studentFilter),
    milestoneSelection,
  )
  const studentMembers = members.filter((member) => member.role === 'STUDENT' && member.userId != null)
  const studentOptions = studentFilterOptions(members, validTasks, (userId) => assigneeLabel(userId, users))

  async function removeTask() {
    if (deleteTarget?.id == null || activeProjectId == null) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteProjectTask(activeProjectId, deleteTarget.id)
      setMessage(`Deleted “${deleteTarget.title ?? 'task'}”. The student account, milestone, and research records were kept.`)
      setDeleteTarget(null)
      window.dispatchEvent(new Event('researchtrace-progress-changed'))
      await load(activeProjectId)
    } catch (cause: unknown) {
      setDeleteError(cause instanceof Error ? cause.message : 'Could not delete task')
    } finally {
      setDeleting(false)
    }
  }

  async function saveDraft(event: React.FormEvent) {
    event.preventDefault()
    if (draft == null || activeProjectId == null) return
    const validation = validateTaskDraft(draft.title ?? '')
    if (validation) {
      setFormError(validation)
      return
    }
    setFormError(null)
    try {
      if (draft.id == null) {
        await createProjectTask(activeProjectId, {
          title: draft.title?.trim() ?? '',
          description: draft.description ?? null,
          assigneeId: draft.assigneeId ?? null,
          milestoneId: draft.milestoneId ?? null,
          dueDate: draft.dueDate ?? null,
          status: draft.status ?? 'TODO',
        })
      } else {
        await updateProjectTask({ ...draft, projectId: activeProjectId, title: draft.title?.trim() ?? '' })
      }
      setDraft(null)
      await load(activeProjectId)
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not save task')
    }
  }

  return (
    <section>
      <PageHeader
        title="Tasks"
        description="Accepted tasks, student assignment, research motivation, and status updates."
      />
      {projectsLoading || (loading && activeProjectId != null) ? (
        <p className="mt-6 text-sm text-stone-600">Loading tasks for the selected project…</p>
      ) : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && projectProgress != null ? (
        <p className="mt-4 text-sm text-stone-700" data-testid="project-progress-label">
          Project progress from the backend: {projectProgress.toFixed(2)}%
        </p>
      ) : null}
      {!loading && usersUnavailable ? <UsersUnavailableNotice /> : null}
      {message ? <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800" role="status">{message}</p> : null}
      {!loading && error ? <Alert message={error} /> : null}
      {!projectsLoading && activeProjectId != null && !loading ? (
        <div className="mt-6 flex flex-wrap gap-2">
          <button type="button" className="rounded-md bg-stone-950 px-3 py-2 text-sm text-white" onClick={() => setDraft({ id: null, projectId: activeProjectId, title: '', description: '', status: 'TODO', assigneeId: null, milestoneId: null, dueDate: null })}>Create task</button>
          {(['ALL', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'UNASSIGNED'] as TaskListFilter[]).map((item) => (
            <button key={item} type="button" className="rounded-md border border-stone-300 px-2 py-1 text-xs" onClick={() => setFilter(item)}>{item}</button>
          ))}
          <label className="text-xs text-stone-600">Student
            <select className="ml-2 rounded-md border border-stone-300 bg-white px-2 py-1" value={studentFilter} onChange={(event) => setStudentFilter(event.target.value)}>
              <option value="">All</option>
              {studentOptions.length === 0 ? <option value="" disabled>No students in this project</option> : null}
              {studentOptions.map((option) => <option key={option.userId} value={option.userId}>{option.label}</option>)}
            </select>
          </label>
          <label className="text-xs text-stone-600">Milestone
            <select className="ml-2 rounded-md border border-stone-300 bg-white px-2 py-1" value={milestoneFilter} onChange={(event) => setMilestoneFilter(event.target.value)}>
              <option value="">All</option>
              <option value="none">No milestone</option>
              {milestones.filter((milestone) => milestone.id != null).map((milestone) => <option key={milestone.id} value={milestone.id ?? ''}>{milestone.title}</option>)}
            </select>
          </label>
        </div>
      ) : null}
      {draft && activeProjectId != null ? (
        <form className="mt-4 grid gap-3 rounded-lg border border-stone-200 bg-white p-4 sm:grid-cols-2" onSubmit={(event) => void saveDraft(event)}>
          <label className="text-xs font-medium text-stone-600">Title
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={draft.title ?? ''} onChange={(event) => setDraft({ ...draft, title: event.target.value })} />
          </label>
          <label className="text-xs font-medium text-stone-600">Description
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={draft.description ?? ''} onChange={(event) => setDraft({ ...draft, description: event.target.value })} />
          </label>
          <label className="text-xs font-medium text-stone-600">Assignee
            <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={draft.assigneeId ?? ''} onChange={(event) => setDraft({ ...draft, assigneeId: event.target.value === '' ? null : Number(event.target.value) })}>
              <option value="">Unassigned</option>
              {studentMembers.map((member) => <option key={member.userId} value={member.userId ?? ''}>{assigneeLabel(member.userId, users)}</option>)}
            </select>
          </label>
          <label className="text-xs font-medium text-stone-600">Milestone
            <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={draft.milestoneId ?? ''} onChange={(event) => setDraft({ ...draft, milestoneId: event.target.value === '' ? null : Number(event.target.value) })}>
              <option value="">No milestone</option>
              {milestones.filter((milestone) => milestone.id != null).map((milestone) => <option key={milestone.id} value={milestone.id ?? ''}>{milestone.title}</option>)}
            </select>
          </label>
          <label className="text-xs font-medium text-stone-600">Due date
            <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" type="date" value={draft.dueDate ?? ''} onChange={(event) => setDraft({ ...draft, dueDate: event.target.value || null })} />
          </label>
          <label className="text-xs font-medium text-stone-600">Status
            <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={draft.status ?? 'TODO'} onChange={(event) => setDraft({ ...draft, status: event.target.value as TaskStatus })}>
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="COMPLETED">COMPLETED</option>
            </select>
          </label>
          {formError ? <p className="text-sm text-red-700 sm:col-span-2">{formError}</p> : null}
          <div className="flex gap-2 sm:col-span-2">
            <button className="rounded-md bg-stone-950 px-3 py-2 text-sm text-white" type="submit">Save task</button>
            <button className="rounded-md border border-stone-300 px-3 py-2 text-sm" type="button" onClick={() => setDraft(null)}>Cancel</button>
          </div>
        </form>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && !error && visibleTasks.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No tasks match this project and filter.</p>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && !error && visibleTasks.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {visibleTasks.map((task) => {
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
                  <p className="text-sm font-medium text-stone-950">
                    <Link className="underline" to={`/tasks/${task.id}`}>{task.title ?? 'Untitled task'}</Link>
                    {isOverdueTask(task, today) ? <span className="ml-2 text-xs font-medium text-red-700">Overdue</span> : null}
                  </p>
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
                <span className="flex items-center gap-2 text-xs font-medium tracking-wide text-stone-500 sm:self-center">
                  <button type="button" className="rounded-md border border-stone-300 px-2 py-1" onClick={() => setDraft(task)}>Edit</button>
                  <button type="button" className="rounded-md border border-red-300 px-2 py-1 text-red-800" onClick={() => { setDeleteError(null); setDeleteTarget(task) }}>Delete</button>
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
      {deleteTarget ? (
        <ConfirmDialog
          title="Delete task"
          message={`Delete “${deleteTarget.title ?? 'this task'}”? Assignee: ${assigneeLabel(deleteTarget.assigneeId, users)}. Status: ${deleteTarget.status ?? 'unknown'}. Due: ${deleteTarget.dueDate ?? 'no due date'}. Task history for this task is removed. The student account, milestone, meeting, and any research decision or experiment stay.`}
          confirmLabel="Delete task"
          busy={deleting}
          error={deleteError}
          onConfirm={() => void removeTask()}
          onCancel={() => { if (!deleting) setDeleteTarget(null) }}
        />
      ) : null}
    </section>
  )
}

function UsersUnavailableNotice() {
  return (
    <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800" role="status">
      Student names could not be loaded, so assignees are shown as “Unknown assignee”.
    </p>
  )
}

function Alert({ message }: { message: string }) {
  return (
    <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      {message}
    </p>
  )
}
