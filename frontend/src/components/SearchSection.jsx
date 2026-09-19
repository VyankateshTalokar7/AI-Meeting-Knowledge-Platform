import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestErrorMessage, searchMeetings } from '../api/client.js'

function renderFormattedText(text) {
  if (!text) return null
  const parts = text.split(/(\*\*.*?\*\*|\*.*?\*|`.*?`)/g)
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length >= 4) {
      return <strong key={index}>{part.slice(2, -2)}</strong>
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length >= 2) {
      return <code key={index} style={{ background: '#f1f5f9', padding: '0.1rem 0.3rem', borderRadius: '4px', fontFamily: 'monospace' }}>{part.slice(1, -1)}</code>
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length >= 2) {
      return <em key={index}>{part.slice(1, -1)}</em>
    }
    return part
  })
}

export default function SearchSection({ token }) {
  const [query, setQuery] = useState('')
  const [result, setResult] = useState(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState('')

  async function handleSearch(event) {
    if (event) event.preventDefault()
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

  function handleChipClick(suggestedQuery) {
    setQuery(suggestedQuery)
  }

  return (
    <section className="search-card">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.25rem' }}>
        <span className="badge" style={{ background: '#d1fae5', color: '#047857', border: '1px solid #6ee7b7' }}>Knowledge Search</span>
        <h2 className="section-title" style={{ margin: 0 }}>Search Meeting Knowledge Base</h2>
      </div>
      <p className="section-subtitle" style={{ marginBottom: '1rem' }}>
        Ask natural-language questions to retrieve semantic answers and exact transcript references across all your meetings.
      </p>

      <form onSubmit={handleSearch}>
        <div className="search-form-row">
          <input
            className="form-control search-input"
            disabled={searching}
            maxLength="1000"
            onChange={(event) => setQuery(event.target.value)}
            placeholder="e.g. What is the source of truth for the platform?"
            required
            type="text"
            value={query}
          />
          <button disabled={searching || !query.trim()} className="btn btn-accent" type="submit">
            {searching ? 'Searching…' : 'Search Knowledge'}
          </button>
        </div>
      </form>

      <div className="prompt-chips">
        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', alignSelf: 'center' }}>Try asking:</span>
        <button type="button" className="chip-btn" onClick={() => handleChipClick('What is the source of truth for the platform?')}>
          "What is the source of truth?"
        </button>
        <button type="button" className="chip-btn" onClick={() => handleChipClick('What vector database is used for semantic search?')}>
          "Which vector database is used?"
        </button>
        <button type="button" className="chip-btn" onClick={() => handleChipClick('What key decisions were made in recent meetings?')}>
          "Key decisions made?"
        </button>
      </div>

      {error && <div className="alert alert-danger" style={{ marginTop: '1.25rem' }} role="alert">{error}</div>}

      {searching && (
        <div style={{ marginTop: '1.5rem', textAlign: 'center', padding: '1.5rem', background: '#ffffff', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
          <p style={{ color: 'var(--teal)', fontWeight: 600, fontSize: '0.9375rem' }}>
            Searching knowledge base...
          </p>
        </div>
      )}

      {result && (
        <div className="answer-card">
          <div className="answer-header">Generated Answer</div>
          <p className="answer-text">{renderFormattedText(result.answer)}</p>

          {result.references && result.references.length > 0 && (
            <div className="references-container">
              <div className="references-title">Sources & References</div>
              <ul className="reference-list">
                {result.references.map((ref, idx) => (
                  <li key={idx} className="reference-tag">
                    <span>Meeting:</span>
                    <Link to={`/meetings/${ref.meeting_id}`}>Meeting #{ref.meeting_id}</Link>
                    {ref.chunk_index != null && <span className="timestamp-pill">Chunk #{ref.chunk_index}</span>}
                    {ref.start_time != null && ref.end_time != null && (
                      <span className="timestamp-pill" style={{ background: '#f0fdf4', color: '#166534', borderColor: '#bbf7d0' }}>
                        {ref.start_time.toFixed(1)}s – {ref.end_time.toFixed(1)}s
                      </span>
                    )}
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
