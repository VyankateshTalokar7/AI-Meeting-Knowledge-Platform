import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestErrorMessage, searchMeetings } from '../api/client.js'

export default function SearchSection({ token }) {
  const [query, setQuery] = useState('')
  const [result, setResult] = useState(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState('')

  async function handleSearch(event) {
    event.preventDefault()
    const trimmedQuery = query.trim()
    if (!trimmedQuery) {
      return
    }

    setError('')
    setSearching(true)
    setResult(null)

    try {
      const response = await searchMeetings(token, trimmedQuery, 5)
      setResult(response)
    } catch (requestError) {
      setError(requestErrorMessage(requestError))
    } finally {
      setSearching(false)
    }
  }

  return (
    <section>
      <h2>Search Meeting Knowledge</h2>
      <form onSubmit={handleSearch}>
        <label>
          Ask a question across your meetings
          <input
            disabled={searching}
            maxLength="1000"
            onChange={(event) => setQuery(event.target.value)}
            placeholder="e.g. What decision was made about the speech-to-text module?"
            required
            type="text"
            value={query}
          />
        </label>
        <button disabled={searching || !query.trim()} type="submit">
          {searching ? 'Searching…' : 'Search'}
        </button>
      </form>

      {error && <p role="alert">{error}</p>}
      {searching && <p>Searching your meeting knowledge base…</p>}

      {!searching && !result && !error && (
        <p>Ask a natural-language question to retrieve semantic answers and references across your meeting transcripts.</p>
      )}

      {result && (
        <div>
          <h3>Answer</h3>
          <p>{result.answer}</p>

          {result.references && result.references.length > 0 && (
            <div>
              <h4>References</h4>
              <ul>
                {result.references.map((ref, idx) => (
                  <li key={idx}>
                    <Link to={`/meetings/${ref.meeting_id}`}>Meeting #{ref.meeting_id}</Link>
                    {ref.chunk_index != null && ` · Chunk #${ref.chunk_index}`}
                    {ref.start_time != null && ref.end_time != null && ` · ${ref.start_time.toFixed(1)}s – ${ref.end_time.toFixed(1)}s`}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </section>
  )
}
