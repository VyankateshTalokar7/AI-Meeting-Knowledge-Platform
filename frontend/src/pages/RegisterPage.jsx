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
    <main>
      <h1>Create account</h1>
      <form onSubmit={handleSubmit}>
        <label>Name<input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
        <label>Email<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Password<input required type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
        {error && <p role="alert">{error}</p>}
        <button disabled={submitting} type="submit">{submitting ? 'Creating account…' : 'Register'}</button>
      </form>
      <p>Already registered? <Link to="/login">Log in</Link></p>
    </main>
  )
}
