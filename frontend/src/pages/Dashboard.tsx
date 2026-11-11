import { useEffect, useState } from 'react'
import { getProjectDashboard } from '../api/dashboard'
import type { ProjectDashboard, User } from '../api/types'
import { getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'
import { DashboardView } from '../components/dashboard/DashboardView'

const SELECTED_PROJECT_ID = 1

export function Dashboard() {
  const [dashboard, setDashboard] = useState<ProjectDashboard | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    function load() {
      Promise.all([
        getProjectDashboard(SELECTED_PROJECT_ID, controller.signal),
        getUsers(controller.signal).catch(() => [] as User[]),
      ])
        .then(([data, loadedUsers]) => {
          if (active && !controller.signal.aborted) {
            setDashboard(data)
            setUsers(loadedUsers)
            setError(null)
          }
        })
        .catch((cause: unknown) => {
          if (!active || controller.signal.aborted) {
            return
          }
          setError(cause instanceof Error ? cause.message : 'Could not load the dashboard')
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
    window.addEventListener('focus', load)
    return () => {
      active = false
      controller.abort()
      window.removeEventListener('researchtrace-progress-changed', load)
      window.removeEventListener('focus', load)
    }
  }, [])

  return (
    <section>
      <PageHeader
        title="Dashboard"
        description="Project progress, student progress, milestones, and tasks for the selected research project."
      />
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading project {SELECTED_PROJECT_ID}…</p> : null}
      {!loading && error ? (
        <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && dashboard ? <DashboardView dashboard={dashboard} users={users} /> : null}
    </section>
  )
}
