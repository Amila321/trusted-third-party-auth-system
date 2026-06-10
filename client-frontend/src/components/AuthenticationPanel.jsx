import { useState } from 'react'
import JsonBlock from './JsonBlock.jsx'

function AuthenticationPanel({ identity, decision, session, busy, onComplete }) {
  const [sessionId, setSessionId] = useState(decision?.session_id || '')
  const [encryptedSessionKey, setEncryptedSessionKey] = useState(decision?.encrypted_session_key_for_user || '')
  const [issuedAt, setIssuedAt] = useState(new Date().toISOString())

  function fillFromDecision() {
    setSessionId(decision?.session_id || '')
    setEncryptedSessionKey(decision?.encrypted_session_key_for_user || '')
    setIssuedAt(decision?.decided_at || new Date().toISOString())
  }

  function handleSubmit(event) {
    event.preventDefault()
    onComplete({ sessionId, encryptedSessionKey, issuedAt })
  }

  return (
    <section className="scenario-panel">
      <div className="section-heading">
        <div>
          <p>Steps 2-3</p>
          <h2>Complete TTP Session</h2>
        </div>
        <span className={session ? 'panel-state complete' : 'panel-state'}>{session ? 'complete' : 'waiting'}</span>
      </div>

      <form className="stacked-form" onSubmit={handleSubmit}>
        <button type="button" className="secondary-button" disabled={!decision?.authenticated} onClick={fillFromDecision}>
          Fill from TTP decision
        </button>
        <label>
          TTP session id
          <input
            value={sessionId}
            onChange={(event) => setSessionId(event.target.value)}
            disabled={!identity}
            placeholder="session id returned by TTP"
          />
        </label>
        <label>
          Encrypted AES key for User
          <textarea
            value={encryptedSessionKey}
            onChange={(event) => setEncryptedSessionKey(event.target.value)}
            disabled={!identity}
            placeholder="encrypted_session_key_for_user"
            rows={4}
          />
        </label>
        <label>
          Issued at
          <input
            value={issuedAt}
            onChange={(event) => setIssuedAt(event.target.value)}
            disabled={!identity}
          />
        </label>
        <button type="submit" disabled={busy || !identity || !sessionId.trim() || !encryptedSessionKey.trim()}>
          Decrypt and store AES session key
        </button>
      </form>

      <JsonBlock title="Client session state" data={session} />
    </section>
  )
}

export default AuthenticationPanel
