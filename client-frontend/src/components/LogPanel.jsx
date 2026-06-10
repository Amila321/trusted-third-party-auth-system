function LogPanel({ events, onClear }) {
  return (
    <section className="log-panel" aria-label="Protocol event log">
      <div className="section-heading">
        <div>
          <p>Event log</p>
          <h2>Visible protocol trace</h2>
        </div>
        <button type="button" className="ghost-button" onClick={onClear} disabled={events.length === 0}>
          Clear
        </button>
      </div>

      <div className="log-list">
        {events.length === 0 && (
          <div className="empty-state">
            Run a scenario step to record the HTTP request, service decision, and cryptographic artifact.
          </div>
        )}

        {events.map((event) => (
          <article className={`log-entry ${event.kind}`} key={event.id}>
            <header>
              <span>{event.time}</span>
              <strong>{event.title}</strong>
            </header>
            <p>{event.message}</p>
            {event.payload && <pre>{JSON.stringify(event.payload, null, 2)}</pre>}
          </article>
        ))}
      </div>
    </section>
  )
}

export default LogPanel
