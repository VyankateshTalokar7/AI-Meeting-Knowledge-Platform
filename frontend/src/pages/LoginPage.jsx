import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { loginUser, requestErrorMessage } from '../api/client.js'

export default function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const response = await loginUser(form)
      localStorage.setItem('authToken', response.token)
      navigate('/dashboard')
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-header">
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <div className="brand-icon">AI</div>
          <span style={{ fontWeight: '700', fontSize: '1rem', color: '#0d9488' }}>Knowledge Platform</span>
        </div>
        <h1>AI-Powered Meeting Platform</h1>
        <p>Transcribe audio, extract knowledge, and perform RAG-based semantic search across your meetings.</p>
      </div>

      <div className="auth-card">
        <h2 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1.25rem', color: 'var(--primary)' }}>Sign in to your account</h2>

        {error && <div className="alert alert-danger" role="alert">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email-input">Email address</label>
            <input
              id="email-input"
              className="form-control"
              required
              type="email"
              placeholder="name@company.com"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
            />
          </div>
          <div className="form-group">
            <label htmlFor="password-input">Password</label>
            <input
              id="password-input"
              className="form-control"
              required
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
            />
          </div>
          <button disabled={submitting} className="btn btn-primary" style={{ width: '100%', marginTop: '0.5rem' }} type="submit">
            {submitting ? 'Authenticating…' : 'Sign In'}
          </button>
        </form>
        <p style={{ marginTop: '1.25rem', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Don't have an account? <Link to="/register" style={{ color: 'var(--accent)', fontWeight: '600', textDecoration: 'none' }}>Register here</Link>
        </p>
      </div>
    </div>
  )
}
