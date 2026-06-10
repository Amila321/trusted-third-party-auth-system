import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8082',
  timeout: 10000,
})

export async function getSystemStatus() {
  const response = await api.get('/api/client/status')
  return response.data
}

export async function registerUser(identityName) {
  const request = { identity_name: identityName }
  const response = await api.post('/api/client/auth/initiate', request)
  return { request, response: response.data }
}

export async function registerServer(identityName) {
  const request = { identity_name: identityName }
  const response = await api.post('/api/client/server/register', request)
  return { request, response: response.data }
}

export async function getServerIdentity() {
  const response = await api.get('/api/client/server/identity')
  return response.data
}

export async function requestSession() {
  const response = await api.post('/api/client/auth/request-session')
  return { request: {}, response: response.data }
}

export async function requestAndCompleteSession() {
  const response = await api.post('/api/client/auth/request-and-complete-session')
  return { request: {}, response: response.data }
}

export async function completeAuthentication({ sessionId, encryptedSessionKey, issuedAt }) {
  const request = {
    session_id: sessionId,
    encrypted_session_key: encryptedSessionKey,
    issued_at: issuedAt,
  }
  const response = await api.post('/api/client/auth/complete', request)
  return { request, response: response.data }
}

export async function sendEncryptedData({ sessionId, plaintext }) {
  const request = {
    session_id: sessionId,
    plaintext,
  }
  const response = await api.post('/api/client/data/encrypt-and-send', request)
  return { request, response: response.data }
}

export async function closeSession(sessionId) {
  const request = { session_id: sessionId }
  const response = await api.post('/api/client/session/close', request)
  return { request, response: response.data }
}

export function explainApiError(error) {
  if (error.response?.data) {
    if (error.response.status === 404 && error.response.data.path?.startsWith('/api/client/')) {
      return {
        ...error.response.data,
        message: 'Client-backend does not expose this endpoint in the currently running process.',
        details:
          'Restart or rebuild client-backend so the Phase 6/7 controllers are loaded, then retry the scenario step.',
      }
    }
    return error.response.data
  }
  return {
    error_code: 'CLIENT_FRONTEND_ERROR',
    message: error.message || 'Request failed',
    details: 'The client frontend could not complete the HTTP request.',
    timestamp: new Date().toISOString(),
  }
}
