import type { ReactNode } from 'react'
import type {
  Meeting,
  Milestone,
  ProjectDashboard,
  StudentProgressItem,
  Task,
  TaskSuggestion,
} from '../../api/types'

type DashboardViewProps = {
  dashboard: ProjectDashboard
}

export function DashboardView({ dashboard }: DashboardViewProps) {
  const project = dashboard.project
  const progressByMilestone = new Map(
    dashboard.milestoneProgress.map((item) => [item.milestoneId, item.progress]),
  )
  const meetings = [...dashboard.meetings].sort((left, right) =>
    (right.meetingDate ?? '').localeCompare(left.meetingDate ?? ''),
  )

  return (
    <div className="mt-6 space-y-6">
      <section className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-stone-950">
              {project?.name ?? 'Untitled project'}
            </h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">
              {project?.description ?? 'No description'}
            </p>
          </div>
          <span className="w-fit rounded-full bg-stone-950 px-3 py-1 text-xs font-medium tracking-wide text-stone-50">
            {project?.status ?? 'No status'}
          </span>
        </div>
        <ProgressMeter label="Project progress" value={dashboard.projectProgress} />
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <Panel title="Student progress">
          {dashboard.studentProgress.length === 0 ? (
            <Empty>No student progress for this project.</Empty>
          ) : (
            <ul className="space-y-4">
              {dashboard.studentProgress.map((item) => (
                <StudentProgressRow key={item.studentId ?? 'unknown'} item={item} />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Milestones">
          {dashboard.milestones.length === 0 ? (
            <Empty>No milestones for this project.</Empty>
          ) : (
            <ul className="space-y-4">
              {dashboard.milestones.map((milestone) => (
                <MilestoneRow
                  key={milestone.id ?? milestone.title}
                  milestone={milestone}
                  progress={progressByMilestone.get(milestone.id) ?? null}
                />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Meetings">
          {meetings.length === 0 ? (
            <Empty>No meetings for this project.</Empty>
          ) : (
            <ul className="divide-y divide-stone-100">
              {meetings.map((meeting) => (
                <MeetingRow key={meeting.id ?? meeting.title} meeting={meeting} />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Tasks">
          {dashboard.tasks.length === 0 ? (
            <Empty>No tasks for this project.</Empty>
          ) : (
            <ul className="divide-y divide-stone-100">
              {dashboard.tasks.map((task) => (
                <TaskRow key={task.id ?? task.title} task={task} />
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <Panel title="Pending task suggestions">
        {dashboard.pendingTaskSuggestions.length === 0 ? (
          <Empty>No pending suggestions.</Empty>
        ) : (
          <ul className="divide-y divide-stone-100">
            {dashboard.pendingTaskSuggestions.map((suggestion) => (
              <SuggestionRow key={suggestion.id ?? suggestion.title} suggestion={suggestion} />
            ))}
          </ul>
        )}
      </Panel>
    </div>
  )
}

function ProgressMeter({ label, value }: { label: string; value: number | null }) {
  const width = value == null ? 0 : Math.min(100, Math.max(0, value))
  return (
    <div className="mt-5">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-sm font-medium text-stone-700">{label}</p>
        <p className="text-sm font-semibold text-stone-950">{formatProgress(value)}</p>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-stone-200">
        <div className="h-full rounded-full bg-stone-950" style={{ width: `${width}%` }} />
      </div>
    </div>
  )
}

function StudentProgressRow({ item }: { item: StudentProgressItem }) {
  return (
    <li>
      <p className="text-sm font-medium text-stone-900">Student {item.studentId ?? 'unknown'}</p>
      <ProgressMeter label="Progress" value={item.progress} />
    </li>
  )
}

function MilestoneRow({
  milestone,
  progress,
}: {
  milestone: Milestone
  progress: number | null | undefined
}) {
  return (
    <li>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-stone-900">{milestone.title ?? 'Untitled milestone'}</p>
        <span className="text-xs font-medium tracking-wide text-stone-500">
          {milestone.status ?? 'No status'}
        </span>
      </div>
      <ProgressMeter label="Progress" value={progress ?? null} />
    </li>
  )
}

function MeetingRow({ meeting }: { meeting: Meeting }) {
  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <p className="text-sm font-medium text-stone-900">{meeting.title ?? 'Untitled meeting'}</p>
      <p className="text-xs text-stone-500">{meeting.meetingDate ?? 'No date'}</p>
    </li>
  )
}

function TaskRow({ task }: { task: Task }) {
  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <div>
        <p className="text-sm font-medium text-stone-900">{task.title ?? 'Untitled task'}</p>
        <p className="text-xs text-stone-500">
          Assignee {task.assigneeId ?? 'none'} · Milestone {task.milestoneId ?? 'none'}
          {task.dueDate ? ` · Due ${task.dueDate}` : ''}
        </p>
      </div>
      <span className="text-xs font-medium tracking-wide text-stone-500">{task.status ?? 'No status'}</span>
    </li>
  )
}

function SuggestionRow({ suggestion }: { suggestion: TaskSuggestion }) {
  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <div>
        <p className="text-sm font-medium text-stone-900">{suggestion.title ?? 'Untitled suggestion'}</p>
        <p className="text-xs text-stone-500">
          {suggestion.assigneeName ?? (suggestion.assigneeId != null ? `Student ${suggestion.assigneeId}` : 'Unassigned')}
        </p>
      </div>
      <span className="text-xs font-medium tracking-wide text-stone-500">{suggestion.status ?? 'PENDING'}</span>
    </li>
  )
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <h2 className="text-sm font-semibold tracking-tight text-stone-950">{title}</h2>
      <div className="mt-4">{children}</div>
    </section>
  )
}

function Empty({ children }: { children: ReactNode }) {
  return <p className="text-sm text-stone-500">{children}</p>
}

function formatProgress(value: number | null): string {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  return `${value.toFixed(2)}%`
}
