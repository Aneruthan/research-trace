type NoActiveProjectProps = {
  loading: boolean
  error: string | null
}

export function NoActiveProject({ loading, error }: NoActiveProjectProps) {
  if (loading) {
    return <p className="mt-6 text-sm text-stone-600">Loading projects…</p>
  }
  if (error) {
    return (
      <p className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
        {error}
      </p>
    )
  }
  return (
    <p className="mt-6 text-sm text-stone-600">
      No active project is selected. Create a project on the Projects page before loading project data.
    </p>
  )
}
