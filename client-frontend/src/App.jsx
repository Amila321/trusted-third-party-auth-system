import { useEffect, useState } from 'react'
import axios from 'axios'
import './App.css'

function ServiceCard({ title, service, status }) {
  return (
      <div className="service-card">
        <h2>{title}</h2>
        <p><strong>Service:</strong> {service}</p>
        <p>
          <strong>Status:</strong>{' '}
          <span className={status === 'UP' ? 'status-up' : 'status-down'}>
          {status}
        </span>
        </p>
      </div>
  )
}

function App() {
  const [systemStatus, setSystemStatus] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const response = await axios.get('http://localhost:8082/api/client/status')
        setSystemStatus(response.data)
      } catch (err) {
        setError('Failed to fetch system status from client-backend.')
      }
    }

    fetchStatus()
  }, [])

  return (
      <div className="app">
        <div className="container">
          <h1>SCS System Status</h1>

          {error && <p className="error">{error}</p>}

          {!systemStatus && !error && <p>Loading...</p>}

          {systemStatus && (
              <div className="services-grid">
                <ServiceCard
                    title="Client Backend"
                    service={systemStatus.clientBackend.service}
                    status={systemStatus.clientBackend.status}
                />
                <ServiceCard
                    title="Server Service"
                    service={systemStatus.serverService.service}
                    status={systemStatus.serverService.status}
                />
                <ServiceCard
                    title="TTP Service"
                    service={systemStatus.ttpService.service}
                    status={systemStatus.ttpService.status}
                />
              </div>
          )}
        </div>
      </div>
  )
}

export default App