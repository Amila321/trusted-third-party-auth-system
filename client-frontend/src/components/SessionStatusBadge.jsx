function SessionStatusBadge({ identity, session }) {
  let label = 'No identity'
  let tone = 'idle'

  if (identity && !session) {
    label = 'Registered, waiting for session key'
    tone = 'pending'
  }

  if (identity && session) {
    label = 'Authenticated session active'
    tone = 'ready'
  }

  return (
    <div className={`session-badge ${tone}`}>
      <span>{label}</span>
      {session?.session_id && <strong>{session.session_id}</strong>}
    </div>
  )
}

export default SessionStatusBadge
