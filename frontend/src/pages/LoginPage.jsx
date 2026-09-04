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
    <main>
      <h1>Log in</h1>
      <form onSubmit={handleSubmit}>
        <label>Email<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Password<input required type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
        {error && <p role="alert">{error}</p>}
        <button disabled={submitting} type="submit">{submitting ? 'Logging in…' : 'Log in'}</button>
      </form>
      <p>Need an account? <Link to="/register">Register</Link></p>
    </main>
  )
}
