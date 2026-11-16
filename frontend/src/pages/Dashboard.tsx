import { useEffect, useRef, useState } from 'react'
import { getProjectDashboard } from '../api/dashboard'
import type { ProjectDashboard, User } from '../api/types'
import { getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'
import { DashboardView } from '../components/dashboard/DashboardView'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'

export function Dashboard() {
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [dashboard, setDashboard] = useState<ProjectDashboard | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [usersUnavailable, setUsersUnavailable] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    setDashboard(null)
    setError(null)
    if (projectsLoading) {
      setLoading(true)
      return () => controller.abort()
    }
    if (projectsError || activeProjectId == null) {
      setLoading(false)
      return () => controller.abort()
    }
    const projectId = activeProjectId

    function load() {
      Promise.all([
        getProjectDashboard(projectId, controller.signal),
        getUsers(controller.signal).catch(() => null),
      ])
        .then(([data, loadedUsers]) => {
          if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            return
          }
          setDashboard(data)
          setUsers(loadedUsers ?? [])
          setUsersUnavailable(loadedUsers == null)
          setError(null)
        })
        .catch((cause: unknown) => {
          if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            return
          }
          setError(cause instanceof Error ? cause.message : 'Could not load the dashboard')
        })
        .finally(() => {
          if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
            setLoading(false)
          }
        })
    }

    setLoading(true)
    load()
    window.addEventListener('researchtrace-progress-changed', load)
    window.addEventListener('focus', load)
    return () => {
      controller.abort()
      window.removeEventListener('researchtrace-progress-changed', load)
      window.removeEventListener('focus', load)
    }
  }, [activeProjectId, projectsLoading, projectsError])

  return (
    <section>
      <PageHeader
        title="Dashboard"
        description="Project progress, student progress, milestones, and tasks for the selected research project."
      />
      {projectsLoading || (loading && activeProjectId != null) ? (
        <p className="mt-6 text-sm text-stone-600">Loading the selected project…</p>
      ) : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}
      {!loading && error ? (
        <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && dashboard && usersUnavailable ? (
        <p className="mt-6 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800" role="status">
          Student names could not be loaded, so students and assignees are shown as unknown.
        </p>
      ) : null}
      {!loading && !error && dashboard ? <DashboardView dashboard={dashboard} users={users} /> : null}
    </section>
  )
}
