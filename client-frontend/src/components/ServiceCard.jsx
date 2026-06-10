function ServiceCard({ title, service, status }) {
  const normalizedStatus = status || 'UNKNOWN'
  const stateClass = normalizedStatus === 'UP' ? 'is-up' : 'is-down'

  return (
    <article className="service-card">
      <div>
        <span className={`status-dot ${stateClass}`} />
        <h3>{title}</h3>
      </div>
      <dl>
        <dt>Service</dt>
        <dd>{service || 'not reported'}</dd>
        <dt>Status</dt>
        <dd className={stateClass}>{normalizedStatus}</dd>
      </dl>
    </article>
  )
}

export default ServiceCard
