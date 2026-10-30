import { useState } from 'react'
import type { SuggestionUpdate, TaskSuggestion } from '../../api/types'

type SuggestionCardProps = {
  suggestion: TaskSuggestion
  onSave: (suggestionId: number, update: SuggestionUpdate) => Promise<void>
  onAccept: (suggestionId: number) => Promise<void>
  onReject: (suggestionId: number) => Promise<void>
  onError: (message: string) => void
}

export function SuggestionCard({
  suggestion,
  onSave,
  onAccept,
  onReject,
  onError,
}: SuggestionCardProps) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [title, setTitle] = useState(suggestion.title ?? '')
  const [description, setDescription] = useState(suggestion.description ?? '')
  const [assigneeId, setAssigneeId] = useState(suggestion.assigneeId?.toString() ?? '')
  const [milestoneId, setMilestoneId] = useState(suggestion.milestoneId?.toString() ?? '')
  const [dueDate, setDueDate] = useState(suggestion.dueDate ?? '')

  const pending = suggestion.status === 'PENDING'
  const suggestionId = suggestion.id

  async function run(action: () => Promise<void>) {
    if (suggestionId == null) {
      onError('Suggestion has no id')
      return
    }
    setBusy(true)
    try {
      await action()
      setEditing(false)
    } catch (cause: unknown) {
      onError(cause instanceof Error ? cause.message : 'Suggestion action failed')
    } finally {
      setBusy(false)
    }
  }

  function startEdit() {
    setTitle(suggestion.title ?? '')
    setDescription(suggestion.description ?? '')
    setAssigneeId(suggestion.assigneeId?.toString() ?? '')
    setMilestoneId(suggestion.milestoneId?.toString() ?? '')
    setDueDate(suggestion.dueDate ?? '')
    setEditing(true)
  }

  function save() {
    if (suggestionId == null) {
      return
    }
    const update: SuggestionUpdate = {
      title: title.trim() === '' ? null : title.trim(),
      description: description.trim() === '' ? null : description.trim(),
      assigneeId: parseOptionalId(assigneeId),
      milestoneId: parseOptionalId(milestoneId),
      dueDate: dueDate.trim() === '' ? null : dueDate.trim(),
    }
    void run(() => onSave(suggestionId, update))
  }

  return (
    <li className="rounded-lg border border-stone-200 bg-white p-5">
      {editing ? (
        <form
          className="grid gap-3"
          onSubmit={(event) => {
            event.preventDefault()
            save()
          }}
        >
          <Field label="Title" value={title} onChange={setTitle} />
          <Field label="Description" value={description} onChange={setDescription} />
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Assignee id" value={assigneeId} onChange={setAssigneeId} />
            <Field label="Milestone id" value={milestoneId} onChange={setMilestoneId} />
            <Field label="Due date" value={dueDate} onChange={setDueDate} type="date" />
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="submit"
              className="rounded-md bg-stone-950 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-60"
              disabled={busy}
            >
              Save
            </button>
            <button
              type="button"
              className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-800"
              onClick={() => setEditing(false)}
              disabled={busy}
            >
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
            <h3 className="text-sm font-semibold text-stone-950">{suggestion.title ?? 'Untitled suggestion'}</h3>
            <span className="w-fit rounded-full bg-stone-100 px-2.5 py-1 text-xs font-medium text-stone-700">
              {suggestion.status ?? 'No status'}
            </span>
          </div>
          {suggestion.description ? (
            <p className="mt-2 text-sm leading-6 text-stone-700">{suggestion.description}</p>
          ) : null}
          <dl className="mt-3 grid gap-2 text-xs text-stone-600 sm:grid-cols-2">
            <div>Assignee: {suggestion.assigneeName ?? '—'}{suggestion.assigneeId != null ? ` (${suggestion.assigneeId})` : ''}</div>
            <div>Milestone id: {suggestion.milestoneId ?? '—'}</div>
            <div>Due date: {suggestion.dueDate ?? '—'}</div>
            <div>Confidence: {suggestion.confidence ?? '—'}</div>
          </dl>
          {pending && suggestionId != null ? (
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-800 disabled:opacity-60"
                onClick={startEdit}
                disabled={busy}
              >
                Edit
              </button>
              <button
                type="button"
                className="rounded-md bg-stone-950 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-60"
                onClick={() => void run(() => onAccept(suggestionId))}
                disabled={busy}
              >
                Accept
              </button>
              <button
                type="button"
                className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-800 disabled:opacity-60"
                onClick={() => void run(() => onReject(suggestionId))}
                disabled={busy}
              >
                Reject
              </button>
            </div>
          ) : null}
        </>
      )}
    </li>
  )
}

function Field({
  label,
  value,
  onChange,
  type = 'text',
}: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
}) {
  return (
    <label className="block text-xs font-medium text-stone-600">
      {label}
      <input
        className="mt-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm text-stone-900"
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  )
}

function parseOptionalId(value: string): number | null {
  const trimmed = value.trim()
  if (trimmed === '') {
    return null
  }
  const parsed = Number(trimmed)
  return Number.isInteger(parsed) ? parsed : null
}
