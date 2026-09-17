import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteMeeting,
  deleteMeetingAudio,
  getMeeting,
  getMeetingAudio,
  getMeetingKnowledge,
  getMeetingTranscript,
  requestErrorMessage,
  uploadMeetingAudio,
} from '../api/client.js'

export default function MeetingDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [meeting, setMeeting] = useState(null)
  const [audio, setAudio] = useState(null)
  const [transcript, setTranscript] = useState(null)
  const [knowledge, setKnowledge] = useState(null)
  const [error, setError] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('authToken')
    if (!token) {
      navigate('/login', { replace: true })
      return
    }

    getMeeting(token, id)
      .then(async (currentMeeting) => {
        setMeeting(currentMeeting)
        try {
          setAudio(await getMeetingAudio(token, id))
        } catch (requestError) {
          if (requestError.status !== 404) {
            setError(requestErrorMessage(requestError))
          }
        }
        if (currentMeeting.status === 'COMPLETED') {
          try {
            const [transcriptData, knowledgeData] = await Promise.all([
              getMeetingTranscript(token, id).catch(() => null),
              getMeetingKnowledge(token, id).catch(() => null),
            ])
            setTranscript(transcriptData)
            setKnowledge(knowledgeData)
          } catch {
            // Optional data error
          }
        }
      })
      .catch((requestError) => setError(requestErrorMessage(requestError)))
  }, [id, navigate])

  async function removeMeeting() {
    try {
      await deleteMeeting(localStorage.getItem('authToken'), id)
      navigate('/dashboard')
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    }
  }

  async function uploadAudio(event) {
    event.preventDefault()
    if (!selectedFile) {
      setError('Select an audio file first.')
      return
    }
    setError('')
    setUploading(true)
    setUploadProgress(0)
    try {
      const uploadedAudio = await uploadMeetingAudio(
        localStorage.getItem('authToken'),
        id,
        selectedFile,
        setUploadProgress,
      )
      setAudio(uploadedAudio)
      setSelectedFile(null)
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    } finally {
      setUploading(false)
    }
  }

  async function removeAudio() {
    setError('')
    try {
      await deleteMeetingAudio(localStorage.getItem('authToken'), id)
      setAudio(null)
      setUploadProgress(0)
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

  if (error) {
    return (
      <div className="page-container">
        <Link to="/dashboard" className="back-link">← Back to my meetings</Link>
        <div className="alert alert-danger" role="alert">{error}</div>
      </div>
    )
  }

  if (!meeting) {
    return (
      <div className="page-container" style={{ textAlign: 'center', padding: '4rem 1rem' }}>
        <p style={{ color: 'var(--text-muted)' }}>Loading meeting details…</p>
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
        <Link to="/dashboard" className="btn btn-secondary btn-sm">
          Dashboard
        </Link>
      </header>

      <main className="page-container">
        <Link to="/dashboard" className="back-link">
          ← Back to Dashboard
        </Link>

        {/* Meeting Header Card */}
        <section className="card meeting-header-card">
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--primary)', margin: 0 }}>
                {meeting.title}
              </h1>
              {getStatusBadge(meeting.status)}
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', marginBottom: '0.75rem' }}>
              {meeting.description || 'No description provided.'}
            </p>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'flex', gap: '1rem' }}>
              <span>📅 Meeting Date: {new Date(meeting.meetingDate).toLocaleString()}</span>
              <span>🆔 ID: #{meeting.id}</span>
            </div>
          </div>

          <div>
            <button onClick={removeMeeting} className="btn btn-danger btn-sm" type="button">
              Delete Meeting
            </button>
          </div>
        </section>

        {/* Audio Recording Card */}
        <section className="card">
          <h2 className="section-title">🎙️ Audio Recording</h2>
          {audio ? (
            <div style={{ background: 'var(--bg-main)', border: '1px solid var(--border-color)', padding: '1rem', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--primary)' }}>
                    {audio.originalFilename}
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    Size: {(audio.fileSize / 1024 / 1024).toFixed(2)} MB • Format: <span className="timestamp-pill">{audio.contentType}</span>
                  </div>
                </div>
                <button onClick={removeAudio} className="btn btn-danger btn-sm" type="button">
                  Delete Audio
                </button>
              </div>
            </div>
          ) : (
            <div style={{ padding: '1rem', background: 'var(--bg-main)', borderRadius: 'var(--radius-md)', border: '1px border-dashed var(--border-color)' }}>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                No audio recording has been uploaded for this meeting. Upload an MP3/WAV file to initiate transcription and AI knowledge extraction.
              </p>
              <form onSubmit={uploadAudio}>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <input
                    className="form-control"
                    accept="audio/mpeg,audio/wav,audio/x-wav,audio/mp4,audio/webm,.mp3,.wav,.m4a,.webm"
                    onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                    type="file"
                  />
                </div>
                {uploading && (
                  <div style={{ marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '0.25rem', color: 'var(--text-muted)' }}>
                      <span>Uploading audio file...</span>
                      <span>{uploadProgress}%</span>
                    </div>
                    <div style={{ width: '100%', background: '#e2e8f0', height: '6px', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ width: `${uploadProgress}%`, background: 'var(--accent)', height: '100%', transition: 'width 0.2s ease' }} />
                    </div>
                  </div>
                )}
                <button disabled={uploading || !selectedFile} className="btn btn-accent" type="submit">
                  {uploading ? `Uploading ${uploadProgress}%` : 'Upload & Process Audio'}
                </button>
              </form>
            </div>
          )}
        </section>

        {/* AI Knowledge Extraction Card */}
        {knowledge && (
          <section className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <span className="badge" style={{ background: '#f0fdf4', color: '#047857', border: '1px solid #a7f3d0' }}>✨ AI Analysis</span>
              <h2 className="section-title" style={{ margin: 0 }}>Structured Knowledge Extraction</h2>
            </div>

            {knowledge.summary && (
              <div className="knowledge-box" style={{ marginBottom: '1.25rem', background: '#f8fafc', borderLeft: '4px solid var(--accent)' }}>
                <h4>Executive Summary</h4>
                <p style={{ fontSize: '0.9375rem', color: 'var(--text-main)', lineHeight: 1.6 }}>{knowledge.summary}</p>
              </div>
            )}

            <div className="knowledge-grid">
              {knowledge.keyTopics && knowledge.keyTopics.length > 0 && (
                <div className="knowledge-box">
                  <h4>📌 Key Topics</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {knowledge.keyTopics.map((topic, index) => (
                      <span key={index} className="badge badge-pill" style={{ background: '#ffffff', border: '1px solid var(--border-color)', padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}>
                        {topic}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {knowledge.decisions && knowledge.decisions.length > 0 && (
                <div className="knowledge-box">
                  <h4>✅ Key Decisions</h4>
                  <ul style={{ paddingLeft: '1.25rem', margin: 0, fontSize: '0.875rem', color: 'var(--text-main)' }}>
                    {knowledge.decisions.map((decision, index) => (
                      <li key={index} style={{ marginBottom: '0.5rem' }}>{decision}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {knowledge.actionItems && knowledge.actionItems.length > 0 && (
              <div style={{ marginTop: '1.25rem' }}>
                <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '0.75rem' }}>
                  🎯 Action Items ({knowledge.actionItems.length})
                </h4>
                <ul className="action-items-list">
                  {knowledge.actionItems.map((item, index) => (
                    <li key={index} className="action-item-card">
                      <span className="action-item-task">{item.task}</span>
                      <div className="action-item-meta">
                        {item.assignee && (
                          <span className="badge" style={{ background: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe' }}>
                            👤 {item.assignee}
                          </span>
                        )}
                        {item.dueDate && (
                          <span className="badge" style={{ background: '#fff7ed', color: '#c2410c', border: '1px solid #ffedd5' }}>
                            📅 Due: {item.dueDate}
                          </span>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </section>
        )}

        {/* Meeting Transcript Card */}
        {transcript && (
          <section className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <h2 className="section-title" style={{ margin: 0 }}>📝 Meeting Transcript</h2>
              {transcript.language && (
                <span className="badge badge-pill">Language: {transcript.language.toUpperCase()}</span>
              )}
            </div>

            {transcript.fullText && (
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Full Transcript Text
                </h4>
                <div style={{ background: 'var(--bg-main)', border: '1px solid var(--border-color)', padding: '1rem', borderRadius: 'var(--radius-md)', whiteSpace: 'pre-wrap', fontSize: '0.9375rem', lineHeight: 1.6, maxHeight: '200px', overflowY: 'auto' }}>
                  {transcript.fullText}
                </div>
              </div>
            )}

            {transcript.segments && transcript.segments.length > 0 && (
              <div>
                <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Timestamped Segments ({transcript.segments.length})
                </h4>
                <div className="transcript-segment-list">
                  {transcript.segments.map((seg, index) => (
                    <div key={index} className="transcript-segment-item">
                      <span className="timestamp-pill">
                        {seg.startTime.toFixed(1)}s – {seg.endTime.toFixed(1)}s
                      </span>
                      <span style={{ flex: 1, color: 'var(--text-main)' }}>{seg.text}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}
      </main>
    </div>
  )
}
