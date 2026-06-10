import JsonBlock from './JsonBlock.jsx'

function SessionRequestPanel({ identity, serverIdentity, decision, busy, onRequest, onRequestAndComplete }) {
  const canRequest = Boolean(identity?.identity_id && serverIdentity?.registered)

  return (
    <section className="scenario-panel">
      <div className="section-heading">
        <div>
          <p>Step 3</p>
          <h2>Request Service Session</h2>
        </div>
        <span className={decision?.authenticated ? 'panel-state complete' : 'panel-state'}>
          {decision?.authenticated ? 'accepted' : 'waiting'}
        </span>
      </div>

      <div className="explain-strip">
        The client backend signs a random challenge with the User private key, sends it to server-service,
        and server-service forwards User and Server certificates to TTP.
      </div>

      <div className="button-row">
        <button type="button" disabled={busy || !canRequest} onClick={onRequest}>
          Request service/session
        </button>
        <button type="button" className="secondary-button" disabled={busy || !canRequest} onClick={onRequestAndComplete}>
          Request and complete session
        </button>
      </div>

      <JsonBlock title="TTP authentication decision" data={decision} />
    </section>
  )
}

export default SessionRequestPanel
