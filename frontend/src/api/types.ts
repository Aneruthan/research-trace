export type UserRole = 'PROFESSOR' | 'STUDENT'

export type User = {
  id: number | null
  name: string | null
  email: string | null
  role: UserRole | null
}

export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export type ProjectMemberRole = 'PROFESSOR' | 'STUDENT'

export type MilestoneStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'BLOCKED'
  | 'DEFERRED'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED'

export type TaskSuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type Project = {
  id: number | null
  name: string | null
  description: string | null
  objectives: string | null
  startDate: string | null
  targetDate: string | null
  supervisorId: number | null
  status: ProjectStatus | null
}

export type ProjectWriteRequest = {
  name: string
  description: string | null
  objectives: string | null
  startDate: string | null
  targetDate: string | null
  supervisorId: number | null
}

export type ProjectMember = {
  id: number | null
  projectId: number | null
  userId: number | null
  role: ProjectMemberRole | null
}

export type StudentProgressItem = {
  studentId: number | null
  progress: number | null
}

export type Milestone = {
  id: number | null
  projectId: number | null
  title: string | null
  description: string | null
  status: MilestoneStatus | null
  dueDate: string | null
  weight: number | null
}

export type MilestoneProgressItem = {
  milestoneId: number | null
  progress: number | null
}

export type Meeting = {
  id: number | null
  projectId: number | null
  title: string | null
  meetingDate: string | null
  notes: string | null
}

export type Task = {
  id: number | null
  projectId?: number | null
  milestoneId: number | null
  assigneeId: number | null
  meetingId?: number | null
  taskSuggestionId?: number | null
  researchDecisionId?: number | null
  experimentId?: number | null
  title: string | null
  description: string | null
  status: TaskStatus | null
  dueDate: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type TaskSuggestion = {
  id: number | null
  meetingId: number | null
  projectId: number | null
  title: string | null
  description: string | null
  assigneeName: string | null
  assigneeId: number | null
  milestoneId: number | null
  dueDate: string | null
  sourceExcerpt: string | null
  confidence: number | null
  status: TaskSuggestionStatus | null
  acceptedTaskId: number | null
  researchDecisionId?: number | null
  experimentId?: number | null
}

export type SuggestionUpdate = {
  title: string | null
  description: string | null
  assigneeId: number | null
  milestoneId: number | null
  dueDate: string | null
  researchDecisionId?: number | null
  experimentId?: number | null
}

export type ResearchMetrics = {
  totalDecisions: number
  activeDecisions: number
  totalExperiments: number
  totalResults: number
  totalEvents: number
  totalChanges: number
}

export type ProjectDashboard = {
  project: Project | null
  projectProgress: number | null
  members: ProjectMember[]
  studentProgress: StudentProgressItem[]
  milestones: Milestone[]
  milestoneProgress: MilestoneProgressItem[]
  meetings: Meeting[]
  tasks: Task[]
  pendingTaskSuggestions: TaskSuggestion[]
  researchMetrics?: ResearchMetrics | null
}
