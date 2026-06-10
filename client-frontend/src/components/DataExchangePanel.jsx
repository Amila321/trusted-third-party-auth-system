import { useState } from 'react'
import JsonBlock from './JsonBlock.jsx'

function DataExchangePanel({ session, exchange, busy, onSend, onClose }) {
  const [plaintext, setPlaintext] = useState('Message protected by AES-256-CBC')

  function handleSend(event) {
    event.preventDefault()
    onSend({ sessionId: session?.session_id || '', plaintext })
  }

  return (
    <section className="scenario-panel">
      <div className="section-heading">
        <div>
          <p>Step 4</p>
          <h2>Encrypted Data Exchange</h2>
        </div>
        <span className={exchange ? 'panel-state complete' : 'panel-state'}>{exchange ? 'observed' : 'ready'}</span>
      </div>

      <form className="stacked-form" onSubmit={handleSend}>
        <label>
          Plaintext to protect
          <textarea
            value={plaintext}
            onChange={(event) => setPlaintext(event.target.value)}
            disabled={!session}
            rows={3}
          />
        </label>
        <div className="button-row">
          <button type="submit" disabled={busy || !session || !plaintext.trim()}>
            Encrypt, send, decrypt response
          </button>
          <button type="button" className="secondary-button" disabled={busy || !session} onClick={onClose}>
            Close session
          </button>
        </div>
      </form>

      <div className="process-list">
        <span>Client encrypts plaintext with stored AES session key and fresh IV.</span>
        <span>Server decrypts, processes plaintext, and encrypts its response with a new IV.</span>
        <span>Client decrypts the response and displays the cleartext result.</span>
      </div>

      <JsonBlock title="Encrypted exchange details" data={exchange} />
    </section>
  )
}

export default DataExchangePanel
