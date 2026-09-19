import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createMeeting, deleteMeeting, getCurrentUser, getMeetings, requestErrorMessage } from '../api/client.js'
import SearchSection from '../components/SearchSection.jsx'

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

  function getStatusBadge(status) {
    switch (status) {
      case 'COMPLETED':
        return <span className="badge badge-completed">Completed</span>
      case 'PROCESSING':
        return <span className="badge badge-processing">Processing</span>
      case 'FAILED':
        return <span className="badge badge-failed">Failed</span>
      default:
        return <span className="badge badge-pill">{status}</span>
    }
  }

  if (error || !user) {
    return (
      <div className="auth-wrapper">
        <div className="auth-card" style={{ textAlign: 'center' }}>
          <p style={{ color: 'var(--text-muted)' }}>{error || 'Loading user workspace profile…'}</p>
        </div>
      </div>
    )
  }

  return (
    <div>
      <header className="app-header">
        <Link to="/dashboard" className="brand-badge">
          <div className="brand-icon">AI</div>
          <span>Meeting Knowledge Platform</span>
        </Link>

        <div className="user-nav">
          <div className="user-chip">
            <span className="user-name">{user.name}</span>
            <span className="user-email">{user.email}</span>
          </div>
          <button onClick={logout} className="btn btn-secondary btn-sm" type="button">
            Log out
          </button>
        </div>
      </header>

      <main className="page-container">
        <div style={{ marginBottom: '1.75rem' }}>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--primary)' }}>
            Welcome back, {user.name}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem' }}>
            Manage organizational meeting knowledge, audio transcriptions, and AI search indexing.
          </p>
        </div>

        {/* Semantic Search RAG Area */}
        <SearchSection token={localStorage.getItem('authToken')} />

        <div className="dashboard-grid">
          {/* Create Meeting Card */}
          <section className="card" style={{ height: 'fit-content' }}>
            <h2 className="section-title">Create New Meeting</h2>
            <p className="section-subtitle">Schedule or register a meeting to upload audio recordings.</p>

            <form onSubmit={createNewMeeting}>
              <div className="form-group">
                <label htmlFor="title-input">Meeting Title</label>
                <input
                  id="title-input"
                  className="form-control"
                  required
                  maxLength="200"
                  placeholder="e.g. AI Platform Development Review"
                  value={form.title}
                  onChange={(event) => setForm({ ...form, title: event.target.value })}
                />
              </div>
              <div className="form-group">
                <label htmlFor="desc-input">Description (Optional)</label>
                <textarea
                  id="desc-input"
                  className="form-control"
                  maxLength="2000"
                  placeholder="Brief agenda or summary notes..."
                  value={form.description}
                  onChange={(event) => setForm({ ...form, description: event.target.value })}
                />
              </div>
              <div className="form-group">
                <label htmlFor="date-input">Date and Time</label>
                <input
                  id="date-input"
                  className="form-control"
                  required
                  type="datetime-local"
                  value={form.meetingDate}
                  onChange={(event) => setForm({ ...form, meetingDate: event.target.value })}
                />
              </div>
              <button disabled={submitting} className="btn btn-primary" style={{ width: '100%' }} type="submit">
                {submitting ? 'Creating Meeting…' : 'Create Meeting'}
              </button>
            </form>
          </section>

          {/* My Meetings Card */}
          <section className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h2 className="section-title" style={{ margin: 0 }}>
                My Meetings ({meetings.length})
              </h2>
            </div>

            {error && <div className="alert alert-danger" role="alert">{error}</div>}

            {meetings.length === 0 ? (
              <div style={{ padding: '2rem 1rem', textAlign: 'center', background: 'var(--bg-main)', borderRadius: 'var(--radius-md)', border: '1px border-dashed var(--border-color)' }}>
                <p style={{ color: 'var(--text-muted)' }}>No meetings have been created yet. Create your first meeting above to upload audio recordings.</p>
              </div>
            ) : (
              <ul className="meeting-list">
                {meetings.map((meeting) => (
                  <li key={meeting.id} className="meeting-item">
                    <div className="meeting-info">
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                        <Link to={`/meetings/${meeting.id}`} className="meeting-link">
                          {meeting.title}
                        </Link>
                        {getStatusBadge(meeting.status)}
                      </div>
                      <div className="meeting-meta">
                        <span>{new Date(meeting.meetingDate).toLocaleString()}</span>
                        {meeting.description && <span>• {meeting.description.length > 50 ? `${meeting.description.substring(0, 50)}…` : meeting.description}</span>}
                      </div>
                    </div>

                    <div className="meeting-actions">
                      <Link to={`/meetings/${meeting.id}`} className="btn btn-secondary btn-sm">
                        View Details
                      </Link>
                      <button onClick={() => removeMeeting(meeting.id)} className="btn btn-danger btn-sm" type="button">
                        Delete
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
