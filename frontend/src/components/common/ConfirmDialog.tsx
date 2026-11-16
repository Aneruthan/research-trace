export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  busy,
  blocked,
  error,
  onConfirm,
  onCancel,
}: {
  title: string
  message: string
  confirmLabel: string
  busy?: boolean
  blocked?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-4">
      <div className="w-full max-w-lg rounded-lg bg-white p-6" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <h2 id="confirm-title" className="text-base font-semibold text-stone-950">{title}</h2>
        <p className="mt-2 whitespace-pre-wrap text-sm text-stone-700">{message}</p>
        {error ? <p className="mt-3 text-sm text-red-700" role="alert">{error}</p> : null}
        <div className="mt-4 flex justify-end gap-2">
          <button type="button" className="rounded-md border border-stone-300 px-3 py-1.5 text-sm" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          {blocked ? null : (
            <button
              type="button"
              className="rounded-md bg-red-700 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
              onClick={onConfirm}
              disabled={busy}
            >
              {busy ? 'Deleting…' : confirmLabel}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
