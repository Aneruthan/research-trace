import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProjectStudent, type StudentWork } from '../api/students'
import type { Task } from '../api/types'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { isOverdueTask } from '../work/work'
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
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const today = localIsoDate()

  useEffect(() => {
    const controller = new AbortController()
    setStudent(null)
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
    getProjectStudent(projectId, studentId, controller.signal)
      .then((loaded) => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setStudent(loaded)
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
  }, [activeProjectId, projectsLoading, projectsError, studentId])

  const tasks = student?.tasks ?? []

  return (
    <section>
      <PageHeader title={student?.name ?? 'Student'} description="Profile, membership, and tasks for the selected project." />
      <Link className="mt-4 inline-block text-sm text-stone-700 underline" to="/students">Back to students</Link>
      {projectsLoading || loading ? <p className="mt-6 text-sm text-stone-600">Loading student…</p> : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? <NoActiveProject loading={false} error={projectsError} /> : null}
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
          <TaskGroup title="Overdue" tasks={tasks.filter((task) => isOverdueTask(task, today))} today={today} />
          <TaskGroup title="In progress" tasks={tasks.filter((task) => task.status === 'IN_PROGRESS')} today={today} />
          <TaskGroup title="Pending" tasks={tasks.filter((task) => task.status === 'TODO' && !isOverdueTask(task, today))} today={today} />
          <TaskGroup title="Completed" tasks={tasks.filter((task) => task.status === 'COMPLETED')} today={today} />
        </div>
      ) : null}
    </section>
  )
}

function TaskGroup({ title, tasks, today }: { title: string; tasks: Task[]; today: string }) {
  return (
    <section>
      <h2 className="text-sm font-semibold text-stone-950">{title}</h2>
      {tasks.length === 0 ? <p className="mt-2 text-sm text-stone-500">None</p> : (
        <ul className="mt-2 divide-y divide-stone-200 rounded-lg border border-stone-200 bg-white">
          {tasks.map((task) => (
            <li key={task.id} className="px-4 py-3 text-sm">
              <Link className="font-medium text-stone-950 underline" to={`/tasks/${task.id}`}>{task.title}</Link>
              <p className="text-xs text-stone-500">
                {task.status} · {task.dueDate ? `Due ${task.dueDate}` : 'No due date'}
                {isOverdueTask(task, today) ? ' · Overdue' : ''}
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
