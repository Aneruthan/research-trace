import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProjectMeetings } from '../api/meetings'
import { deleteProjectTask, getProjectTask, getTaskHistory } from '../api/tasks'
import type { Meeting, Task } from '../api/types'
import { assigneeLabel, getUsers } from '../api/users'
import type { User } from '../api/types'
import { ConfirmDialog } from '../components/common/ConfirmDialog'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { localIsoDate } from '../components/common/dates'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { isOverdueTask } from '../work/work'

type HistoryItem = { id: number | null; previousStatus: string | null; newStatus: string | null; changedAt: string | null }

export function TaskDetails() {
  const navigate = useNavigate()
  const { taskId: taskIdParam } = useParams()
  const taskId = Number(taskIdParam)
  const { activeProjectId, activeProject, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [task, setTask] = useState<Task | null>(null)
  const [history, setHistory] = useState<HistoryItem[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    setTask(null)
    setHistory([])
    setError(null)
    if (projectsLoading) {
      setLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null || !Number.isInteger(taskId)) {
      setLoading(false)
      if (!Number.isInteger(taskId)) setError('Task id is not valid')
      return () => controller.abort()
    }
    const projectId = activeProjectId
    setLoading(true)
    Promise.all([
      getProjectTask(projectId, taskId, controller.signal),
      getTaskHistory(projectId, taskId, controller.signal),
      getUsers(controller.signal).catch(() => [] as User[]),
      getProjectMeetings(projectId, controller.signal).catch(() => [] as Meeting[]),
    ]).then(([loadedTask, loadedHistory, loadedUsers, loadedMeetings]) => {
      if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) return
      if (loadedTask.projectId !== projectId) {
        setError('This task does not belong to the selected project.')
        return
      }
      setTask(loadedTask)
      setHistory(loadedHistory)
      setUsers(loadedUsers)
      setMeetings(loadedMeetings)
    }).catch((cause: unknown) => {
      if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) return
      setError(cause instanceof Error ? cause.message : 'Could not load task')
    }).finally(() => {
      if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) setLoading(false)
    })
    return () => controller.abort()
  }, [activeProjectId, projectsLoading, projectsError, taskId])

  const meeting = meetings.find((item) => item.id === task?.meetingId)
  const overdue = task ? isOverdueTask(task, localIsoDate()) : false

  async function removeTask() {
    if (activeProjectId == null || task?.id == null) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteProjectTask(activeProjectId, task.id)
      window.dispatchEvent(new Event('researchtrace-progress-changed'))
      navigate('/tasks')
    } catch (cause: unknown) {
      setDeleteError(cause instanceof Error ? cause.message : 'Could not delete task')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <section>
      <PageHeader title={task?.title ?? 'Task'} description="Task details for the selected project." />
      <div className="mt-4 flex items-center justify-between gap-3">
        <Link className="text-sm text-stone-700 underline" to="/tasks">Back to tasks</Link>
        {task ? <button type="button" className="rounded-md border border-red-300 px-3 py-1.5 text-sm text-red-800" onClick={() => { setDeleteError(null); setConfirming(true) }}>Delete task</button> : null}
      </div>
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading task…</p> : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? <NoActiveProject loading={false} error={projectsError} /> : null}
      {error ? <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}
      {task && !error ? (
        <div className="mt-6 space-y-4 rounded-lg border border-stone-200 bg-white p-5 text-sm text-stone-700">
          <p>{task.description || 'No description'}</p>
          <p>Assignee: {assigneeLabel(task.assigneeId, users)}</p>
          <p>Milestone: {task.milestoneId ?? 'None'}</p>
          <p>Due: {task.dueDate ?? 'No due date'}{overdue ? ' · Overdue' : ''}</p>
          <p>Status: {task.status}</p>
          <p>Source meeting: {meeting ? <Link className="underline" to={`/meetings/${meeting.id}`}>{meeting.title}</Link> : 'Not from a meeting'}</p>
          <h2 className="pt-2 text-sm font-semibold text-stone-950">Status history</h2>
          {history.length === 0 ? <p className="text-stone-500">No status changes recorded.</p> : (
            <ul className="space-y-1 text-xs">
              {history.map((item) => (
                <li key={item.id ?? item.changedAt}>{item.previousStatus ?? '—'} → {item.newStatus} · {item.changedAt ?? ''}</li>
              ))}
            </ul>
          )}
        </div>
      ) : null}
      {confirming && task ? (
        <ConfirmDialog
          title="Delete task"
          message={`Delete “${task.title ?? 'this task'}”? Assignee: ${assigneeLabel(task.assigneeId, users)}. Project: ${activeProject?.name ?? 'the active project'}. Status: ${task.status ?? 'unknown'}. Due: ${task.dueDate ?? 'no due date'}. Task history for this task is removed. The student, milestone, and research records stay.`}
          confirmLabel="Delete task"
          busy={deleting}
          error={deleteError}
          onConfirm={() => void removeTask()}
          onCancel={() => { if (!deleting) setConfirming(false) }}
        />
      ) : null}
    </section>
  )
}
