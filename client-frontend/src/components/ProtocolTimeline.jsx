const steps = [
  {
    id: 'registration',
    title: '1. Registration',
    text: 'Client backend generates RSA-4096 keys, sends the public key to TTP, and receives a TTP-signed certificate.',
  },
  {
    id: 'authentication',
    title: '2. TTP decision',
    text: 'TTP validates certificates and signed challenge, creates one AES-256 session key, and wraps it for User and Server.',
  },
  {
    id: 'completion',
    title: '3. Client completion',
    text: 'Client backend decrypts the user-wrapped session key with its RSA private key and stores the AES key in memory.',
  },
  {
    id: 'exchange',
    title: '4. Encrypted data',
    text: 'Client encrypts plaintext with AES-CBC, Server decrypts/processes it, then returns an encrypted response.',
  },
]

function ProtocolTimeline({ identity, session, exchange }) {
  const statusByStep = {
    registration: identity ? 'complete' : 'current',
    authentication: session ? 'complete' : identity ? 'current' : 'waiting',
    completion: session ? 'complete' : identity ? 'current' : 'waiting',
    exchange: exchange ? 'complete' : session ? 'current' : 'waiting',
  }

  return (
    <section className="timeline-panel">
      <div className="section-heading">
        <div>
          <p>Protocol path</p>
          <h2>TTP authentication flow</h2>
        </div>
      </div>

      <div className="timeline">
        {steps.map((step) => (
          <article className={`timeline-step ${statusByStep[step.id]}`} key={step.id}>
            <span>{statusByStep[step.id]}</span>
            <h3>{step.title}</h3>
            <p>{step.text}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

export default ProtocolTimeline
