import { useState } from 'react'
import JsonBlock from './JsonBlock.jsx'

function ServerRegistrationPanel({ serverIdentity, servicesUp, busy, onRegister }) {
  const [identityName, setIdentityName] = useState('server')

  function handleSubmit(event) {
    event.preventDefault()
    onRegister(identityName)
  }

  return (
    <section className="scenario-panel">
      <div className="section-heading">
        <div>
          <p>Step 2</p>
          <h2>Register Server at TTP</h2>
        </div>
        <span className={serverIdentity?.registered ? 'panel-state complete' : 'panel-state'}>
          {serverIdentity?.registered ? 'complete' : 'ready'}
        </span>
      </div>

      <form className="control-row" onSubmit={handleSubmit}>
        <label>
          Server identity name
          <input value={identityName} onChange={(event) => setIdentityName(event.target.value)} />
        </label>
        <button type="submit" disabled={busy || !servicesUp || !identityName.trim()}>
          Generate server RSA keys and register
        </button>
      </form>

      <div className="process-list">
        <span>client-backend asks server-service to register itself.</span>
        <span>server-service fetches the TTP public key and encrypts its SHA-256 identity id.</span>
        <span>server-service keeps its private RSA key in memory and receives an X.509 certificate.</span>
      </div>

      <JsonBlock title="Server identity response" data={serverIdentity} />
    </section>
  )
}

export default ServerRegistrationPanel
