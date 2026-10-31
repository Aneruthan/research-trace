import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { Dashboard } from './pages/Dashboard'
import { MeetingDetails } from './pages/MeetingDetails'
import { Meetings } from './pages/Meetings'
import { Milestones } from './pages/Milestones'
import { ResearchTrace } from './pages/ResearchTrace'
import { Tasks } from './pages/Tasks'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<Dashboard />} />
          <Route path="meetings" element={<Meetings />} />
          <Route path="meetings/:meetingId" element={<MeetingDetails />} />
          <Route path="tasks" element={<Tasks />} />
          <Route path="milestones" element={<Milestones />} />
          <Route path="research-trace" element={<ResearchTrace />} />
          <Route path="research" element={<Navigate to="/research-trace" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
