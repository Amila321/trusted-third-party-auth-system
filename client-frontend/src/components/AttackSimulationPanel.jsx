import JsonBlock from './JsonBlock.jsx'

const attacks = [
  {
    scenario: 'FORGED_USER_CERTIFICATE',
    label: 'Run forged user certificate attack',
    text: 'Uses the real user id but replaces the certificate with a self-signed attacker certificate.',
  },
  {
    scenario: 'TAMPERED_USER_CERTIFICATE',
    label: 'Run tampered certificate attack',
    text: 'Replaces the registered certificate with another syntactically valid certificate.',
  },
  {
    scenario: 'INVALID_CHALLENGE_SIGNATURE',
    label: 'Run invalid signature attack',
    text: 'Uses the real certificate but signs the challenge with a rogue private key.',
  },
]

function AttackSimulationPanel({ identity, serverIdentity, result, busy, onSimulate }) {
  const canRun = Boolean(identity?.identity_id && serverIdentity?.registered)

  return (
    <section className="scenario-panel attack-panel">
      <div className="section-heading">
        <div>
          <p>Negative validation</p>
          <h2>Attack Simulation</h2>
        </div>
        <span className={result?.rejected ? 'panel-state blocked' : 'panel-state'}>
          {result?.rejected ? 'attack blocked' : 'ready'}
        </span>
      </div>

      <div className="explain-strip">
        These tests still go through the real pipeline: client-backend {'->'} server-service {'->'} TTP.
        A successful defense returns <strong>authenticated=false</strong> and no valid session is created.
      </div>

      <div className="attack-buttons">
        {attacks.map((attack) => (
          <article className="attack-option" key={attack.scenario}>
            <div>
              <strong>{attack.label}</strong>
              <p>{attack.text}</p>
            </div>
            <button type="button" disabled={busy || !canRun} onClick={() => onSimulate(attack.scenario)}>
              Run
            </button>
          </article>
        ))}
      </div>

      {result && (
        <div className={result.rejected ? 'attack-result blocked' : 'attack-result danger'}>
          <strong>{result.rejected ? 'Rejected by TTP' : 'Unexpected authentication'}</strong>
          <span>{result.ttp_decision?.rejection_reason || 'No rejection reason returned'}</span>
        </div>
      )}

      <JsonBlock title="Attack simulation response" data={result} />
    </section>
  )
}

export default AttackSimulationPanel
