import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'

export function AppShell() {
  const [open, setOpen] = useState(false)

  return (
    <div className="min-h-svh bg-stone-100 text-stone-900">
      <div className="flex min-h-svh">
        <aside className="hidden w-64 shrink-0 bg-stone-950 md:block">
          <Sidebar />
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="flex items-center gap-3 border-b border-stone-200 bg-white px-4 py-3 md:hidden">
            <button
              type="button"
              className="rounded-md border border-stone-300 px-3 py-1.5 text-sm font-medium text-stone-800"
              onClick={() => setOpen((current) => !current)}
              aria-expanded={open}
              aria-controls="mobile-nav"
            >
              Menu
            </button>
            <span className="text-sm font-semibold tracking-tight">ResearchTrace</span>
          </header>

          {open ? (
            <div id="mobile-nav" className="border-b border-stone-800 bg-stone-950 md:hidden">
              <Sidebar onNavigate={() => setOpen(false)} />
            </div>
          ) : null}

          <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 lg:px-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
