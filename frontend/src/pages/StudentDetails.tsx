import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProjectMilestones } from '../api/milestones'
import { getProjectStudent, type StudentWork } from '../api/students'
import { createProjectTask } from '../api/tasks'
import type { Milestone, Task, TaskStatus } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { isOverdueTask, studentTaskDraft, studentTaskEmptyMessage, validateStudentTask, type StudentTaskDraft } from '../work/work'
import { localIsoDate } from '../components/common/dates'

export function StudentDetails() {
  const { studentId: studentIdParam } = useParams()
  const studentId = Number(studentIdParam)
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [student, setStudent] = useState<StudentWork | null>(null)
  const [milestones, setMilestones] = useState<Milestone[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [draft, setDraft] = useState<StudentTaskDraft | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const today = localIsoDate()
  const scopeRef = useRef<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const scope = `${activeProjectId ?? 'none'}:${studentId}`
    if (scopeRef.current !== scope) {
      scopeRef.current = scope
      setStudent(null)
      setMilestones([])
      setMessage(null)
      setAdding(false)
      setDraft(null)
      setFormError(null)
    }
    setError(null)
    if (projectsLoading) {
      setLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null || !Number.isInteger(studentId)) {
      setLoading(false)
      if (!Number.isInteger(studentId)) setError('Student id is not valid')
      return () => controller.abort()
    }
    const projectId = activeProjectId
    setLoading(true)
    Promise.all([
      getProjectStudent(projectId, studentId, controller.signal),
      getProjectMilestones(projectId, controller.signal).catch(() => [] as Milestone[]),
    ])
      .then(([loaded, loadedMilestones]) => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setStudent(loaded)
          setMilestones(loadedMilestones)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) return
        setError(cause instanceof Error ? cause.message : 'Could not load student')
      })
      .finally(() => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [activeProjectId, projectsLoading, projectsError, studentId, reloadKey])

  const tasks = student?.tasks ?? []
  const milestoneIds = milestones.flatMap((milestone) => milestone.id == null ? [] : [milestone.id])

  function startAddTask() {
    if (student?.userId == null) return
    setFormError(null)
    setMessage(null)
    setDraft(studentTaskDraft(student.userId))
    setAdding(true)
  }

  async function saveTask(event: React.FormEvent) {
    event.preventDefault()
    if (draft == null || activeProjectId == null || student?.userId == null) return
    const validation = validateStudentTask(
      { ...draft, assigneeId: student.userId },
      { studentIsMember: student.projectId === activeProjectId, milestoneIds },
    )
    if (validation) {
      setFormError(validation)
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await createProjectTask(activeProjectId, {
        title: draft.title.trim(),
        description: draft.description.trim() === '' ? null : draft.description.trim(),
        dueDate: draft.dueDate === '' ? null : draft.dueDate,
        status: draft.status,
        milestoneId: draft.milestoneId,
        assigneeId: student.userId,
      })
      setAdding(false)
      setDraft(null)
      setMessage(`Task created for ${student.name ?? 'this student'}.`)
      window.dispatchEvent(new Event('researchtrace-progress-changed'))
      setReloadKey((current) => current + 1)
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not create task')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section>
      <PageHeader title={student?.name ?? 'Student'} description="Profile, membership, and tasks for the selected project." />
      <Link className="mt-4 inline-block text-sm text-stone-700 underline" to="/students">Back to students</Link>
      {projectsLoading || loading ? <p className="mt-6 text-sm text-stone-600">Loading student…</p> : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? <NoActiveProject loading={false} error={projectsError} /> : null}
      {message ? <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800" role="status">{message}</p> : null}
      {error ? <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}
      {student && !loading && !error ? (
        <div className="mt-6 space-y-6">
          <section className="rounded-lg border border-stone-200 bg-white p-5 text-sm text-stone-700">
            <p>{student.email ?? 'No email'}</p>
            <p className="mt-1">Membership role: {student.role}</p>
            <p className="mt-1">Current project: {student.projectId}</p>
            <p className="mt-1">Projects this user belongs to: {student.projectMembershipCount}</p>
            <p className="mt-1">Progress: {student.progress == null ? '—' : `${student.progress.toFixed(2)}%`}</p>
            <p className="mt-1">{student.completedTasks} completed · {student.inProgressTasks} in progress · {student.pendingTasks} pending · {student.overdueTasks} overdue</p>
          </section>
          <section>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-stone-950">Assigned tasks</h2>
              <button
                type="button"
                className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white"
                onClick={startAddTask}
              >
                + Add Task
              </button>
            </div>
            {adding && draft ? (
              <form className="mt-4 grid gap-3 rounded-lg border border-stone-200 bg-white p-4 sm:grid-cols-2" onSubmit={(event) => void saveTask(event)}>
                <label className="text-xs font-medium text-stone-600 sm:col-span-2">Task title (required)
                  <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} />
                </label>
                <label className="text-xs font-medium text-stone-600 sm:col-span-2">Description
                  <textarea className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" rows={2} value={draft.description} onChange={(event) => setDraft({ ...draft, description: event.target.value })} />
                </label>
                <label className="text-xs font-medium text-stone-600">Due date
                  <input type="date" className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={draft.dueDate} onChange={(event) => setDraft({ ...draft, dueDate: event.target.value })} />
                </label>
                <label className="text-xs font-medium text-stone-600">Status
                  <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as TaskStatus })}>
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="COMPLETED">COMPLETED</option>
                  </select>
                </label>
                <label className="text-xs font-medium text-stone-600">Milestone
                  <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={draft.milestoneId ?? ''} onChange={(event) => setDraft({ ...draft, milestoneId: event.target.value === '' ? null : Number(event.target.value) })}>
                    <option value="">No milestone</option>
                    {milestones.filter((milestone) => milestone.id != null).map((milestone) => (
                      <option key={milestone.id} value={milestone.id ?? ''}>{milestone.title ?? `Milestone ${milestone.id}`}</option>
                    ))}
                  </select>
                </label>
                <label className="text-xs font-medium text-stone-600">Student
                  <input className="mt-1 w-full rounded-md border border-stone-200 bg-stone-50 px-3 py-2 text-sm text-stone-800" value={student.name ?? 'This student'} readOnly />
                </label>
                {formError ? <p className="text-sm text-red-700 sm:col-span-2" role="alert">{formError}</p> : null}
                <div className="flex gap-2 sm:col-span-2">
                  <button type="submit" className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={saving}>
                    {saving ? 'Saving…' : 'Save task'}
                  </button>
                  <button type="button" className="rounded-md border border-stone-300 px-3 py-2 text-sm" onClick={() => { setAdding(false); setDraft(null); setFormError(null) }} disabled={saving}>
                    Cancel
                  </button>
                </div>
              </form>
            ) : null}
            {studentTaskEmptyMessage(tasks.length) ? (
              <p className="mt-4 text-sm text-stone-500">{studentTaskEmptyMessage(tasks.length)}</p>
            ) : (
              <ul className="mt-4 divide-y divide-stone-200 rounded-lg border border-stone-200 bg-white">
                {tasks.map((task) => (
                  <StudentTaskRow key={task.id ?? task.title} task={task} milestones={milestones} today={today} />
                ))}
              </ul>
            )}
          </section>
        </div>
      ) : null}
    </section>
  )
}

function StudentTaskRow({ task, milestones, today }: { task: Task; milestones: Milestone[]; today: string }) {
  const milestone = task.milestoneId == null
    ? null
    : milestones.find((item) => item.id === task.milestoneId)?.title ?? `Milestone ${task.milestoneId}`
  return (
    <li className="px-4 py-3 text-sm">
      <Link className="font-medium text-stone-950 underline" to={`/tasks/${task.id}`}>{task.title}</Link>
      <p className="text-xs text-stone-500">
        {task.status ?? 'No status'}
        {' · '}
        {task.dueDate ? `Due ${task.dueDate}` : 'No due date'}
        {milestone ? ` · ${milestone}` : ''}
        {isOverdueTask(task, today) ? ' · Overdue' : ''}
      </p>
    </li>
  )
}
