import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createMeeting, deleteMeeting, getCurrentUser, getMeetings, requestErrorMessage } from '../api/client.js'

export default function DashboardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [meetings, setMeetings] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({ title: '', description: '', meetingDate: '' })
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('authToken')
    if (!token) {
      navigate('/login', { replace: true })
      return
    }

    Promise.all([getCurrentUser(token), getMeetings(token)])
      .then(([currentUser, currentMeetings]) => {
        setUser(currentUser)
        setMeetings(currentMeetings)
      })
      .catch((requestError) => {
        localStorage.removeItem('authToken')
        setError(requestErrorMessage(requestError))
        navigate('/login', { replace: true })
      })
  }, [navigate])

  function logout() {
    localStorage.removeItem('authToken')
    navigate('/login')
  }

  async function createNewMeeting(event) {
    event.preventDefault()
    const token = localStorage.getItem('authToken')
    setError('')
    setSubmitting(true)
    try {
      const meeting = await createMeeting(token, {
        ...form,
        meetingDate: new Date(form.meetingDate).toISOString(),
      })
      setMeetings((currentMeetings) => [meeting, ...currentMeetings])
      setForm({ title: '', description: '', meetingDate: '' })
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  async function removeMeeting(id) {
    const token = localStorage.getItem('authToken')
    setError('')
    try {
      await deleteMeeting(token, id)
      setMeetings((currentMeetings) => currentMeetings.filter((meeting) => meeting.id !== id))
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    }
  }

  if (error || !user) {
    return <main><p>{error || 'Loading your profile…'}</p></main>
  }

  return (
    <main>
      <h1>Welcome, {user.name}</h1>
      <p>{user.email}</p>
      <button onClick={logout} type="button">Log out</button>

      <section>
        <h2>Create meeting</h2>
        <form onSubmit={createNewMeeting}>
          <label>Title<input required maxLength="200" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
          <label>Description<textarea maxLength="2000" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
          <label>Meeting date<input required type="datetime-local" value={form.meetingDate} onChange={(event) => setForm({ ...form, meetingDate: event.target.value })} /></label>
          <button disabled={submitting} type="submit">{submitting ? 'Creating…' : 'Create meeting'}</button>
        </form>
      </section>

      <section>
        <h2>My meetings</h2>
        {error && <p role="alert">{error}</p>}
        {meetings.length === 0 ? <p>No meetings have been created yet.</p> : (
          <ul>
            {meetings.map((meeting) => (
              <li key={meeting.id}>
                <Link to={`/meetings/${meeting.id}`}>{meeting.title}</Link>
                {' — '}{new Date(meeting.meetingDate).toLocaleString()} ({meeting.status})
                <button onClick={() => removeMeeting(meeting.id)} type="button">Delete</button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
