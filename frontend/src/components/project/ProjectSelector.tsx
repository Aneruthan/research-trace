import { useActiveProject } from '../../project/ActiveProjectContext'

export function ProjectSelector() {
  const { selectable, activeProjectId, loading, error, selectProject } = useActiveProject()

  return (
    <div className="px-3 pb-2">
      <label htmlFor="active-project" className="block px-1 text-xs font-medium text-stone-400">
        Active project
      </label>
      {loading ? <p className="mt-2 px-1 text-sm text-stone-400">Loading projects…</p> : null}
      {!loading && error ? (
        <p className="mt-2 px-1 text-sm text-red-300" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && selectable.length === 0 ? (
        <p className="mt-2 px-1 text-sm text-stone-400">No active projects</p>
      ) : null}
      {!loading && !error && selectable.length > 0 ? (
        <select
          id="active-project"
          className="mt-2 w-full rounded-md border border-stone-700 bg-stone-900 px-2 py-2 text-sm text-stone-50"
          value={activeProjectId ?? ''}
          onChange={(event) => selectProject(Number(event.target.value))}
        >
          {selectable.map((project) => (
            <option key={project.id} value={project.id ?? ''}>
              {project.name ?? 'Untitled project'}
            </option>
          ))}
        </select>
      ) : null}
    </div>
  )
}
