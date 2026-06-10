import { useState } from 'react'
import JsonBlock from './JsonBlock.jsx'

function AuthenticationPanel({ identity, session, busy, onComplete }) {
  const [sessionId, setSessionId] = useState('')
  const [encryptedSessionKey, setEncryptedSessionKey] = useState('')
  const [issuedAt, setIssuedAt] = useState(new Date().toISOString())

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

      <div className="explain-strip">
        <strong>Visible handoff:</strong> after Server calls TTP, paste the TTP `session_id` and
        `encrypted_session_key_for_user` here. Client backend decrypts it with the private RSA key it generated in Step 1.
      </div>

      <form className="stacked-form" onSubmit={handleSubmit}>
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
