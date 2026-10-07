import { NavLink } from 'react-router-dom'

const links = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/meetings', label: 'Meetings', end: false },
  { to: '/tasks', label: 'Tasks', end: false },
  { to: '/milestones', label: 'Milestones', end: false },
  { to: '/research-trace', label: 'Research Trace', end: false },
]

type SidebarProps = {
  onNavigate?: () => void
}

export function Sidebar({ onNavigate }: SidebarProps) {
  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-stone-800 px-5 py-5">
        <p className="text-xs font-medium tracking-[0.18em] text-stone-400 uppercase">
          CS5013
        </p>
        <p className="mt-1 text-lg font-semibold tracking-tight text-stone-50">
          ResearchTrace
        </p>
      </div>
      <nav className="flex flex-1 flex-col gap-1 px-3 py-4">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.end}
            onClick={onNavigate}
            className={({ isActive }) =>
              [
                'rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-stone-800 text-white'
                  : 'text-stone-300 hover:bg-stone-800/70 hover:text-white',
              ].join(' ')
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
