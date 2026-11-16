import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjectStudents, type StudentWork } from '../api/students'
import type { User } from '../api/types'
import {
  addProjectMember,
  createUser,
  getUsers,
  removeProjectMember,
  updateProjectMember,
  updateUser,
} from '../api/users'
import { PageHeader } from '../components/common/PageHeader'
import { NoActiveProject } from '../components/project/NoActiveProject'
import { useActiveProject } from '../project/ActiveProjectContext'
import { isCurrentProjectResponse } from '../project/projectSelection'
import { validatePerson } from '../work/work'

export function Students() {
  const { activeProjectId, loading: projectsLoading, error: projectsError } = useActiveProject()
  const activeProjectIdRef = useRef(activeProjectId)
  useEffect(() => {
    activeProjectIdRef.current = activeProjectId
  }, [activeProjectId])
  const [students, setStudents] = useState<StudentWork[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [existingUserId, setExistingUserId] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editEmail, setEditEmail] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    setStudents([])
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
    setLoading(true)
    Promise.all([getProjectStudents(projectId, controller.signal), getUsers(controller.signal)])
      .then(([loadedStudents, loadedUsers]) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) return
        setStudents(loadedStudents)
        setUsers(loadedUsers)
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted || !isCurrentProjectResponse(projectId, activeProjectIdRef.current)) return
        setError(cause instanceof Error ? cause.message : 'Could not load students')
      })
      .finally(() => {
        if (!controller.signal.aborted && isCurrentProjectResponse(projectId, activeProjectIdRef.current)) {
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [activeProjectId, projectsLoading, projectsError])

  async function reload() {
    if (activeProjectId == null) return
    const [loadedStudents, loadedUsers] = await Promise.all([
      getProjectStudents(activeProjectId),
      getUsers(),
    ])
    if (!isCurrentProjectResponse(activeProjectId, activeProjectIdRef.current)) return
    setStudents(loadedStudents)
    setUsers(loadedUsers)
  }

  async function createAndAdd(event: React.FormEvent) {
    event.preventDefault()
    if (activeProjectId == null) return
    const validation = validatePerson(name, email)
    if (validation) {
      setError(validation)
      return
    }
    setSaving(true)
    setError(null)
    try {
      const user = await createUser({ name: name.trim(), email: email.trim(), role: 'STUDENT' })
      if (user.id == null) throw new Error('Created student has no id')
      await addProjectMember(activeProjectId, { userId: user.id, role: 'STUDENT' })
      setName('')
      setEmail('')
      setMessage(`${user.name} was created and added to this project.`)
      await reload()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not create student')
    } finally {
      setSaving(false)
    }
  }

  async function addExisting(event: React.FormEvent) {
    event.preventDefault()
    if (activeProjectId == null || existingUserId === '') return
    setSaving(true)
    setError(null)
    try {
      await addProjectMember(activeProjectId, { userId: Number(existingUserId), role: 'STUDENT' })
      setExistingUserId('')
      setMessage('Student added to this project.')
      await reload()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not add student')
    } finally {
      setSaving(false)
    }
  }

  async function saveStudent(student: StudentWork) {
    if (student.userId == null) return
    const validation = validatePerson(editName, editEmail)
    if (validation) {
      setError(validation)
      return
    }
    setSaving(true)
    setError(null)
    try {
      await updateUser(student.userId, { name: editName.trim(), email: editEmail.trim(), role: 'STUDENT' })
      setEditingId(null)
      setMessage('Student details saved.')
      await reload()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not update student')
    } finally {
      setSaving(false)
    }
  }

  async function removeStudent(student: StudentWork) {
    if (activeProjectId == null || student.membershipId == null) return
    if (!window.confirm(`Remove ${student.name ?? 'this student'} from the project? Their user record and tasks will be kept.`)) {
      return
    }
    setError(null)
    try {
      await removeProjectMember(activeProjectId, student.membershipId)
      setMessage('Membership removed. The user and their task history were kept.')
      await reload()
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not remove membership')
    }
  }

  const memberIds = new Set(students.map((student) => student.userId))
  const availableUsers = users.filter((user) => user.role === 'STUDENT' && user.id != null && !memberIds.has(user.id))

  return (
    <section>
      <PageHeader
        title="Students"
        description="Students are global users. Membership connects a user to the selected project."
      />
      {projectsLoading || (loading && activeProjectId != null) ? (
        <p className="mt-6 text-sm text-stone-600">Loading students for the selected project…</p>
      ) : null}
      {!projectsLoading && (projectsError || activeProjectId == null) ? (
        <NoActiveProject loading={false} error={projectsError} />
      ) : null}
      {message ? <Notice>{message}</Notice> : null}
      {error ? <Alert message={error} /> : null}
      {!projectsLoading && activeProjectId != null && !loading ? (
        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <form className="rounded-lg border border-stone-200 bg-white p-4" onSubmit={(event) => void createAndAdd(event)}>
            <h2 className="text-sm font-semibold text-stone-950">Create student</h2>
            <label className="mt-3 block text-xs font-medium text-stone-600">
              Name
              <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={name} onChange={(event) => setName(event.target.value)} />
            </label>
            <label className="mt-3 block text-xs font-medium text-stone-600">
              Email
              <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <button className="mt-3 rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-60" disabled={saving} type="submit">
              Create and add
            </button>
          </form>
          <form className="rounded-lg border border-stone-200 bg-white p-4" onSubmit={(event) => void addExisting(event)}>
            <h2 className="text-sm font-semibold text-stone-950">Add an existing student</h2>
            <label className="mt-3 block text-xs font-medium text-stone-600">
              Student
              <select className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm" value={existingUserId} onChange={(event) => setExistingUserId(event.target.value)}>
                <option value="">Select a student</option>
                {availableUsers.map((user) => (
                  <option key={user.id} value={user.id ?? ''}>{user.name} ({user.email})</option>
                ))}
              </select>
            </label>
            <button className="mt-3 rounded-md border border-stone-300 bg-white px-3 py-2 text-sm font-medium text-stone-800" disabled={saving || existingUserId === ''} type="submit">
              Add to project
            </button>
          </form>
        </div>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && students.length === 0 ? (
        <p className="mt-6 text-sm text-stone-500">No students are members of this project.</p>
      ) : null}
      {!projectsLoading && activeProjectId != null && !loading && students.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {students.map((student) => (
            <li key={student.membershipId ?? student.userId} className="px-4 py-4">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <Link className="text-sm font-medium text-stone-950 underline" to={`/students/${student.userId}`}>
                    {student.name ?? 'Unknown student'}
                  </Link>
                  <p className="text-xs text-stone-500">{student.email ?? 'No email'}</p>
                  <p className="mt-1 text-xs text-stone-600">
                    {student.role} · {student.totalTasks} tasks · {student.completedTasks} completed · {student.inProgressTasks} in progress · {student.pendingTasks} pending · {student.overdueTasks} overdue
                    {' · '}
                    Progress {student.progress == null ? '—' : `${student.progress.toFixed(2)}%`}
                    {student.projectMembershipCount > 1 ? ` · Member of ${student.projectMembershipCount} projects` : ''}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button type="button" className="rounded-md border border-stone-300 px-2 py-1 text-xs" onClick={() => {
                    setEditingId(student.userId)
                    setEditName(student.name ?? '')
                    setEditEmail(student.email ?? '')
                  }}>Edit</button>
                  {student.membershipId != null && activeProjectId != null ? (
                    <button type="button" className="rounded-md border border-stone-300 px-2 py-1 text-xs" onClick={() => void updateProjectMember(activeProjectId, student.membershipId as number, { role: student.role === 'PROFESSOR' ? 'STUDENT' : 'PROFESSOR' }).then(() => reload())}>
                      Role: {student.role}
                    </button>
                  ) : null}
                  <button type="button" className="rounded-md border border-red-200 px-2 py-1 text-xs text-red-800" onClick={() => void removeStudent(student)}>Remove</button>
                </div>
              </div>
              {editingId === student.userId ? (
                <form className="mt-3 grid gap-3 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); void saveStudent(student) }}>
                  <label className="text-xs font-medium text-stone-600">Name
                    <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={editName} onChange={(event) => setEditName(event.target.value)} />
                  </label>
                  <label className="text-xs font-medium text-stone-600">Email
                    <input className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm" value={editEmail} onChange={(event) => setEditEmail(event.target.value)} />
                  </label>
                  <button className="w-fit rounded-md bg-stone-950 px-3 py-2 text-sm text-white" type="submit">Save student</button>
                </form>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}

function Notice({ children }: { children: string }) {
  return <p className="mt-6 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800">{children}</p>
}

function Alert({ message }: { message: string }) {
  return <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{message}</p>
}
