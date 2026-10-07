import { useParams } from 'react-router-dom'
import { PageHeader } from '../components/common/PageHeader'

export function MeetingDetails() {
  const { meetingId } = useParams()

  return (
    <section>
      <PageHeader
        title="Meeting"
        description="Transcript, extracted task suggestions, and human review."
      />
      <p className="mt-6 text-sm text-stone-600">
        Meeting {meetingId ?? 'unknown'}
      </p>
    </section>
  )
}
