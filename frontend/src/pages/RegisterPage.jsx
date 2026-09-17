import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser, requestErrorMessage } from '../api/client.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await registerUser(form)
      navigate('/login')
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
        <p>Create your account to start managing meeting transcripts and AI knowledge extraction.</p>
      </div>

      <div className="auth-card">
        <h2 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1.25rem', color: 'var(--primary)' }}>Create your account</h2>

        {error && <div className="alert alert-danger" role="alert">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name-input">Full name</label>
            <input
              id="name-input"
              className="form-control"
              required
              type="text"
              placeholder="Alex Johnson"
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </div>
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
            {submitting ? 'Creating account…' : 'Register Account'}
          </button>
        </form>
        <p style={{ marginTop: '1.25rem', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Already have an account? <Link to="/login" style={{ color: 'var(--accent)', fontWeight: '600', textDecoration: 'none' }}>Sign in here</Link>
        </p>
      </div>
    </div>
  )
}
