import { useState } from 'react'
import JsonBlock from './JsonBlock.jsx'

function RegistrationPanel({ identity, busy, onRegister }) {
  const [identityName, setIdentityName] = useState('alice')

  function handleSubmit(event) {
    event.preventDefault()
    onRegister(identityName)
  }

  return (
    <section className="scenario-panel">
      <div className="section-heading">
        <div>
          <p>Step 1</p>
          <h2>Register User at TTP</h2>
        </div>
        <span className={identity ? 'panel-state complete' : 'panel-state'}>{identity ? 'complete' : 'ready'}</span>
      </div>

      <form className="control-row" onSubmit={handleSubmit}>
        <label>
          User identity name
          <input
            value={identityName}
            onChange={(event) => setIdentityName(event.target.value)}
            placeholder="alice"
          />
        </label>
        <button type="submit" disabled={busy || !identityName.trim()}>
          Generate RSA keys and register
        </button>
      </form>

      <div className="process-list">
        <span>Client backend generates RSA-4096 key pair.</span>
        <span>TTP receives public key and stores identity in memory.</span>
        <span>TTP returns identity id and X.509 certificate PEM.</span>
      </div>

      <JsonBlock title="Latest registration response" data={identity} />
    </section>
  )
}

export default RegistrationPanel
