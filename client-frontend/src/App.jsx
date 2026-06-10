import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  closeSession,
  completeAuthentication,
  explainApiError,
  getSystemStatus,
  registerUser,
  sendEncryptedData,
} from './api/clientApi.js'
import AuthenticationPanel from './components/AuthenticationPanel.jsx'
import DataExchangePanel from './components/DataExchangePanel.jsx'
import LogPanel from './components/LogPanel.jsx'
import ProtocolTimeline from './components/ProtocolTimeline.jsx'
import RegistrationPanel from './components/RegistrationPanel.jsx'
import ServiceCard from './components/ServiceCard.jsx'
import SessionStatusBadge from './components/SessionStatusBadge.jsx'

const emptyStatus = {
  clientBackend: { service: 'client-backend', status: 'UNKNOWN' },
  serverService: { service: 'server-service', status: 'UNKNOWN' },
  ttpService: { service: 'ttp-service', status: 'UNKNOWN' },
}

function App() {
  const [systemStatus, setSystemStatus] = useState(emptyStatus)
  const [identity, setIdentity] = useState(null)
  const [session, setSession] = useState(null)
  const [exchange, setExchange] = useState(null)
  const [events, setEvents] = useState([])
  const [busyStep, setBusyStep] = useState('')

  const services = useMemo(
    () => [
      ['Client Backend', systemStatus.clientBackend],
      ['Server Service', systemStatus.serverService],
      ['TTP Service', systemStatus.ttpService],
    ],
    [systemStatus],
  )

  const addEvent = useCallback((kind, title, message, payload) => {
    setEvents((current) => [
      {
        id: `${Date.now()}-${Math.random()}`,
        kind,
        title,
        message,
        payload,
        time: new Date().toLocaleTimeString(),
      },
      ...current,
    ])
  }, [])

  async function runStep(step, action) {
    setBusyStep(step)
    try {
      return await action()
    } finally {
      setBusyStep('')
    }
  }

  const refreshStatus = useCallback(async () => {
    try {
      const status = await getSystemStatus()
      setSystemStatus(status)
      addEvent('info', 'Status refreshed', 'Client backend checked all service health endpoints.', status)
    } catch (error) {
      const payload = explainApiError(error)
      addEvent('error', 'Status request failed', 'The frontend could not reach client-backend status endpoint.', payload)
    }
  }, [addEvent])

  useEffect(() => {
    refreshStatus()
  }, [refreshStatus])

  async function handleRegister(identityName) {
    await runStep('registration', async () => {
      addEvent('info', 'Registration started', 'Client backend will generate RSA keys and register the public key at TTP.', {
        identity_name: identityName,
      })
      try {
        const result = await registerUser(identityName)
        setIdentity(result.response)
        setSession(null)
        setExchange(null)
        addEvent('success', 'TTP registration completed', 'TTP returned an identity id and signed X.509 certificate.', result)
      } catch (error) {
        addEvent('error', 'Registration failed', 'TTP registration did not complete.', explainApiError(error))
      }
    })
  }

  async function handleComplete(payload) {
    await runStep('authentication', async () => {
      addEvent('info', 'Session completion started', 'Client backend will RSA-decrypt the user-wrapped AES session key.', payload)
      try {
        const result = await completeAuthentication(payload)
        setSession(result.response)
        setExchange(null)
        addEvent('success', 'AES session key stored', 'Client backend decrypted and stored the AES-256 session key.', result)
      } catch (error) {
        addEvent('error', 'Session completion failed', 'Client backend could not complete authentication.', explainApiError(error))
      }
    })
  }

  async function handleSendData(payload) {
    await runStep('exchange', async () => {
      addEvent('info', 'Encrypted exchange started', 'Client backend will encrypt plaintext before sending it to server-service.', payload)
      try {
        const result = await sendEncryptedData(payload)
        setExchange(result.response)
        addEvent('success', 'Encrypted exchange completed', 'Server response was decrypted by client-backend.', result)
      } catch (error) {
        addEvent('error', 'Encrypted exchange failed', 'Client-server AES exchange did not complete.', explainApiError(error))
      }
    })
  }

  async function handleCloseSession() {
    if (!session?.session_id) {
      return
    }

    await runStep('close', async () => {
      addEvent('info', 'Session close started', 'Client backend will close local state and ask server-service to close the session.', {
        session_id: session.session_id,
      })
      try {
        const result = await closeSession(session.session_id)
        setSession(null)
        setExchange(null)
        addEvent('success', 'Session closed', 'Client and server session state was cleared.', result)
      } catch (error) {
        addEvent('error', 'Session close failed', 'The session close request did not complete.', explainApiError(error))
      }
    })
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Security of Computer Systems</p>
          <h1>SCS TTP Authentication Demo</h1>
          <p className="hero-copy">
            Observe registration, certificate issuance, TTP session-key distribution, AES encrypted transport,
            and the exact JSON artifacts moving through each service.
          </p>
        </div>
        <SessionStatusBadge identity={identity} session={session} />
      </header>

      <section className="workspace">
        <aside className="left-rail">
          <div className="section-heading compact-heading">
            <div>
              <p>Runtime</p>
              <h2>Service status</h2>
            </div>
            <button type="button" className="ghost-button" onClick={refreshStatus} disabled={busyStep !== ''}>
              Refresh
            </button>
          </div>

          <div className="services-stack">
            {services.map(([title, service]) => (
              <ServiceCard title={title} service={service.service} status={service.status} key={title} />
            ))}
          </div>

          <ProtocolTimeline identity={identity} session={session} exchange={exchange} />
        </aside>

        <section className="scenario-grid">
          <RegistrationPanel identity={identity} busy={busyStep === 'registration'} onRegister={handleRegister} />
          <AuthenticationPanel
            identity={identity}
            session={session}
            busy={busyStep === 'authentication'}
            onComplete={handleComplete}
          />
          <DataExchangePanel
            session={session}
            exchange={exchange}
            busy={busyStep === 'exchange' || busyStep === 'close'}
            onSend={handleSendData}
            onClose={handleCloseSession}
          />
        </section>
      </section>

      <LogPanel events={events} onClear={() => setEvents([])} />
    </main>
  )
}

export default App
