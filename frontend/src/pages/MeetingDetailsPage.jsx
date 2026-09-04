import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteMeeting,
  deleteMeetingAudio,
  getMeeting,
  getMeetingAudio,
  requestErrorMessage,
  uploadMeetingAudio,
} from '../api/client.js'

export default function MeetingDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [meeting, setMeeting] = useState(null)
  const [audio, setAudio] = useState(null)
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

  if (error) {
    return <main><p role="alert">{error}</p><Link to="/dashboard">Back to my meetings</Link></main>
  }

  if (!meeting) {
    return <main><p>Loading meeting…</p></main>
  }

  return (
    <main>
      <Link to="/dashboard">Back to my meetings</Link>
      <h1>{meeting.title}</h1>
      <p>{meeting.description || 'No description provided.'}</p>
      <p>Meeting date: {new Date(meeting.meetingDate).toLocaleString()}</p>
      <p>Status: {meeting.status}</p>

      <section>
        <h2>Audio recording</h2>
        {audio ? (
          <>
            <p>Audio uploaded: {audio.originalFilename}</p>
            <p>Size: {(audio.fileSize / 1024 / 1024).toFixed(2)} MB</p>
            <p>Format: {audio.contentType}</p>
            <button onClick={removeAudio} type="button">Delete audio</button>
          </>
        ) : (
          <>
            <p>No audio recording has been uploaded.</p>
            <form onSubmit={uploadAudio}>
              <label>
                Select audio file
                <input
                  accept="audio/mpeg,audio/wav,audio/x-wav,audio/mp4,audio/webm,.mp3,.wav,.m4a,.webm"
                  onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                  type="file"
                />
              </label>
              <button disabled={uploading} type="submit">{uploading ? `Uploading ${uploadProgress}%` : 'Upload audio'}</button>
            </form>
          </>
        )}
      </section>
      <button onClick={removeMeeting} type="button">Delete meeting</button>
    </main>
  )
}
