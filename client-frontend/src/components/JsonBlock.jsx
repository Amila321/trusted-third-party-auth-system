function JsonBlock({ title, data, compact = false }) {
  if (!data) {
    return null
  }

  return (
    <div className={`json-block ${compact ? 'compact' : ''}`}>
      <div className="json-title">{title}</div>
      <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
  )
}

export default JsonBlock
