import { useEffect, useState } from 'react'
import { archiveProject, createProject, updateProject } from '../api/projects'
import { getProjectProgress } from '../api/tasks'
import type { Project, User } from '../api/types'
import { getUsers } from '../api/users'
import { PageHeader } from '../components/common/PageHeader'
import { formatDateOrTimestamp } from '../components/common/dates'
import { useActiveProject } from '../project/ActiveProjectContext'
import {
  archiveConfirmation,
  emptyProjectForm,
  projectToForm,
  toProjectWriteRequest,
  validateProjectForm,
  type ProjectFormValues,
} from '../project/projectForm'
import { visibleProjects } from '../project/projectSelection'

export function Projects() {
  const { projects, loading, error, refreshProjects, selectProject, activeProjectId } = useActiveProject()
  const [users, setUsers] = useState<User[]>([])
  const [usersError, setUsersError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const [progressById, setProgressById] = useState<Record<number, number | null>>({})
  const [mode, setMode] = useState<'closed' | 'create' | 'edit'>('closed')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<ProjectFormValues>(emptyProjectForm)
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [archiveTarget, setArchiveTarget] = useState<Project | null>(null)
  const [archiving, setArchiving] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getUsers(controller.signal)
      .then((loaded) => {
        if (!controller.signal.aborted) {
          setUsers(loaded)
          setUsersError(null)
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setUsersError(cause instanceof Error ? cause.message : 'Could not load users')
      })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (loading || error) {
      return
    }
    const controller = new AbortController()
    const ids = projects.map((project) => project.id).filter((id): id is number => id != null)
    Promise.all(
      ids.map(async (id) => {
        try {
          return [id, await getProjectProgress(id, controller.signal)] as const
        } catch {
          return [id, null] as const
        }
      }),
    ).then((entries) => {
      if (controller.signal.aborted) {
        return
      }
      setProgressById(Object.fromEntries(entries))
    })
    return () => controller.abort()
  }, [projects, loading, error])

  const listed = visibleProjects(projects, showArchived)

  function openCreate() {
    setMode('create')
    setEditingId(null)
    setForm(emptyProjectForm)
    setFormError(null)
    setMessage(null)
  }

  function openEdit(project: Project) {
    if (project.id == null) {
      return
    }
    setMode('edit')
    setEditingId(project.id)
    setForm(projectToForm(project))
    setFormError(null)
    setMessage(null)
  }

  async function submitForm(event: React.FormEvent) {
    event.preventDefault()
    const validation = validateProjectForm(form)
    if (validation) {
      setFormError(validation)
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const request = toProjectWriteRequest(form)
      if (mode === 'edit' && editingId != null) {
        await updateProject(editingId, request)
        await refreshProjects(editingId)
        setMessage('Project updated.')
      } else {
        const created = await createProject(request)
        await refreshProjects(created.id)
        setMessage('Project created.')
      }
      setMode('closed')
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not save the project')
    } finally {
      setSaving(false)
    }
  }

  async function confirmArchive() {
    if (archiveTarget?.id == null) {
      return
    }
    setArchiving(true)
    setFormError(null)
    try {
      await archiveProject(archiveTarget.id)
      await refreshProjects()
      setMessage(`${archiveTarget.name ?? 'Project'} archived. Its records were kept.`)
      setArchiveTarget(null)
    } catch (cause: unknown) {
      setFormError(cause instanceof Error ? cause.message : 'Could not archive the project')
    } finally {
      setArchiving(false)
    }
  }

  return (
    <section>
      <PageHeader
        title="Projects"
        description="Create and maintain independent research projects. Choose the active project from the sidebar."
      />

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <button
          type="button"
          className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white"
          onClick={openCreate}
        >
          Create project
        </button>
        <label className="flex items-center gap-2 text-sm text-stone-700">
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(event) => setShowArchived(event.target.checked)}
          />
          Show archived projects
        </label>
      </div>

      {message ? (
        <p className="mt-4 rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800" role="status">
          {message}
        </p>
      ) : null}
      {loading ? <p className="mt-6 text-sm text-stone-600">Loading projects…</p> : null}
      {!loading && error ? (
        <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
      {usersError ? (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800" role="status">
          Supervisors could not be loaded, so names may be missing. {usersError}
        </p>
      ) : null}

      {mode !== 'closed' ? (
        <ProjectForm
          mode={mode}
          form={form}
          users={users}
          saving={saving}
          formError={formError}
          onChange={setForm}
          onSubmit={(event) => void submitForm(event)}
          onCancel={() => setMode('closed')}
        />
      ) : null}

      {!loading && !error && listed.length === 0 ? (
        <p className="mt-6 text-sm text-stone-600">
          {showArchived ? 'No projects yet.' : 'No current projects. Create one, or show archived projects.'}
        </p>
      ) : null}

      {!loading && !error && listed.length > 0 ? (
        <ul className="mt-6 divide-y divide-stone-200 overflow-hidden rounded-lg border border-stone-200 bg-white">
          {listed.map((project) => {
            const progress = project.id == null ? null : progressById[project.id]
            const supervisor = users.find((user) => user.id === project.supervisorId)
            const isActive = project.id === activeProjectId
            return (
              <li key={project.id ?? project.name} className="px-4 py-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-sm font-semibold text-stone-950">{project.name ?? 'Untitled project'}</h2>
                      <span className="rounded-full bg-stone-100 px-2 py-0.5 text-xs font-medium text-stone-700">
                        {project.status ?? 'No status'}
                      </span>
                      {isActive ? (
                        <span className="rounded-full bg-stone-950 px-2 py-0.5 text-xs font-medium text-stone-50">
                          Active
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-2 text-sm text-stone-600">{project.description ?? 'No description'}</p>
                    <p className="mt-1 text-sm text-stone-600">
                      Objectives: {project.objectives ?? 'None recorded'}
                    </p>
                    <p className="mt-1 text-xs text-stone-500">
                      {formatDateOrTimestamp(project.startDate) ?? 'No start date'}
                      {' – '}
                      {formatDateOrTimestamp(project.targetDate) ?? 'No target date'}
                    </p>
                    <p className="mt-1 text-xs text-stone-500">
                      Supervisor: {supervisor?.name ?? (project.supervisorId == null ? 'Unassigned' : 'Unknown supervisor')}
                    </p>
                    <p className="mt-1 text-xs text-stone-500">
                      Progress: {progress == null ? 'Unavailable' : `${progress}%`}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {project.status !== 'ARCHIVED' && project.id != null ? (
                      <button
                        type="button"
                        className="rounded-md border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-800"
                        onClick={() => selectProject(project.id as number)}
                      >
                        Open
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="rounded-md border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-800"
                      onClick={() => openEdit(project)}
                    >
                      Edit
                    </button>
                    {project.status !== 'ARCHIVED' ? (
                      <button
                        type="button"
                        className="rounded-md border border-stone-300 bg-white px-2.5 py-1 text-xs font-medium text-stone-800"
                        onClick={() => {
                          setFormError(null)
                          setArchiveTarget(project)
                        }}
                      >
                        Archive
                      </button>
                    ) : null}
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      ) : null}

      {archiveTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-4">
          <div className="w-full max-w-md rounded-lg bg-white p-6" role="dialog" aria-modal="true" aria-labelledby="archive-title">
            <h2 id="archive-title" className="text-base font-semibold text-stone-950">Archive project</h2>
            <p className="mt-2 text-sm text-stone-600">{archiveConfirmation(archiveTarget.name)}</p>
            {formError ? <p className="mt-3 text-sm text-red-700" role="alert">{formError}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                className="rounded-md border border-stone-300 px-3 py-1.5 text-sm"
                onClick={() => setArchiveTarget(null)}
                disabled={archiving}
              >
                Cancel
              </button>
              <button
                type="button"
                className="rounded-md bg-stone-950 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
                onClick={() => void confirmArchive()}
                disabled={archiving}
              >
                {archiving ? 'Archiving…' : 'Archive project'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}

function ProjectForm({
  mode,
  form,
  users,
  saving,
  formError,
  onChange,
  onSubmit,
  onCancel,
}: {
  mode: 'create' | 'edit'
  form: ProjectFormValues
  users: User[]
  saving: boolean
  formError: string | null
  onChange: (values: ProjectFormValues) => void
  onSubmit: (event: React.FormEvent) => void
  onCancel: () => void
}) {
  return (
    <form className="mt-6 grid gap-3 rounded-lg border border-stone-200 bg-white p-5" onSubmit={onSubmit}>
      <h2 className="text-sm font-semibold text-stone-950">{mode === 'create' ? 'New project' : 'Edit project'}</h2>
      {formError ? <p className="text-sm text-red-700" role="alert">{formError}</p> : null}
      <Field label="Project name" required>
        <input
          className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm"
          value={form.name}
          onChange={(event) => onChange({ ...form, name: event.target.value })}
          required
        />
      </Field>
      <Field label="Description">
        <textarea
          className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm"
          rows={2}
          value={form.description}
          onChange={(event) => onChange({ ...form, description: event.target.value })}
        />
      </Field>
      <Field label="Research objectives">
        <textarea
          className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm"
          rows={3}
          value={form.objectives}
          onChange={(event) => onChange({ ...form, objectives: event.target.value })}
        />
      </Field>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Start date">
          <input
            type="date"
            className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm"
            value={form.startDate}
            onChange={(event) => onChange({ ...form, startDate: event.target.value })}
          />
        </Field>
        <Field label="Target completion date">
          <input
            type="date"
            className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm"
            value={form.targetDate}
            onChange={(event) => onChange({ ...form, targetDate: event.target.value })}
          />
        </Field>
      </div>
      <Field label="Supervisor">
        <select
          className="mt-1 w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-sm"
          value={form.supervisorId}
          onChange={(event) => onChange({ ...form, supervisorId: event.target.value })}
        >
          <option value="">Unassigned</option>
          {users.filter((user) => user.id != null).map((user) => (
            <option key={user.id} value={user.id ?? ''}>
              {user.name ?? 'Unnamed user'}{user.role ? ` (${user.role})` : ''}
            </option>
          ))}
        </select>
      </Field>
      <div className="flex gap-2">
        <button
          type="submit"
          className="rounded-md bg-stone-950 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
          disabled={saving}
        >
          {saving ? 'Saving…' : mode === 'create' ? 'Create project' : 'Save changes'}
        </button>
        <button type="button" className="rounded-md border border-stone-300 px-3 py-2 text-sm" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="block text-xs font-medium text-stone-700">
      {label}{required ? ' (required)' : ''}
      {children}
    </label>
  )
}
