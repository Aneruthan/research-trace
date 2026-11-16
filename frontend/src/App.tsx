import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { Dashboard } from './pages/Dashboard'
import { MeetingDetails } from './pages/MeetingDetails'
import { Meetings } from './pages/Meetings'
import { Milestones } from './pages/Milestones'
import { Projects } from './pages/Projects'
import { ResearchTrace } from './pages/ResearchTrace'
import { StudentDetails } from './pages/StudentDetails'
import { Students } from './pages/Students'
import { TaskDetails } from './pages/TaskDetails'
import { Tasks } from './pages/Tasks'
import { ActiveProjectProvider } from './project/ActiveProjectContext'

export default function App() {
  return (
    <BrowserRouter>
      <ActiveProjectProvider>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<Dashboard />} />
            <Route path="projects" element={<Projects />} />
            <Route path="students" element={<Students />} />
            <Route path="students/:studentId" element={<StudentDetails />} />
            <Route path="meetings" element={<Meetings />} />
            <Route path="meetings/:meetingId" element={<MeetingDetails />} />
            <Route path="tasks" element={<Tasks />} />
            <Route path="tasks/:taskId" element={<TaskDetails />} />
            <Route path="milestones" element={<Milestones />} />
            <Route path="research-trace" element={<ResearchTrace />} />
            <Route path="research" element={<Navigate to="/research-trace" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </ActiveProjectProvider>
    </BrowserRouter>
  )
}
