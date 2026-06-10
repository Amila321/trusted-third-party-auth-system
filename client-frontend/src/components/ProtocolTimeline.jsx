const steps = [
  {
    id: 'registration',
    title: '1. Registration',
    text: 'Client backend generates RSA-4096 keys, sends the public key to TTP, and receives a TTP-signed certificate.',
  },
  {
    id: 'server',
    title: '2. Server registration',
    text: 'Server generates RSA-4096 keys, encrypts its identity id with the TTP public key, and receives a certificate.',
  },
  {
    id: 'decision',
    title: '3. TTP decision',
    text: 'TTP validates certificates and signed challenge, creates one AES-256 session key, and wraps it for User and Server.',
  },
  {
    id: 'completion',
    title: '4. Client completion',
    text: 'Client backend decrypts the user-wrapped session key with its RSA private key and stores the AES key in memory.',
  },
  {
    id: 'exchange',
    title: '5. Encrypted data',
    text: 'Client encrypts plaintext with AES-CBC, Server decrypts/processes it, then returns an encrypted response.',
  },
]

function ProtocolTimeline({ identity, serverIdentity, decision, session, exchange }) {
  const statusByStep = {
    registration: identity ? 'complete' : 'current',
    server: serverIdentity?.registered ? 'complete' : identity ? 'current' : 'waiting',
    decision: decision?.authenticated ? 'complete' : identity && serverIdentity?.registered ? 'current' : 'waiting',
    completion: session ? 'complete' : decision?.authenticated ? 'current' : 'waiting',
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
