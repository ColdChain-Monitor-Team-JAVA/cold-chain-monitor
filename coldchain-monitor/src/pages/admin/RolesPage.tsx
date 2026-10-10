import { ROLES } from '../../data/demoUsers'
import { Panel } from '../../components/ui'

export default function RolesPage() {
  return (
    <div className="max-w-2xl space-y-4">
      <header>
        <h1 className="text-2xl font-semibold text-navy-900">Role Management</h1>
        <p className="text-sm text-slate-500">Role configuration will be available in a later development stage.</p>
      </header>
      <Panel title="System roles">
        <ul className="divide-y divide-slate-100 text-sm">{ROLES.map((r) => <li key={r} className="py-2">{r}</li>)}</ul>
      </Panel>
    </div>
  )
}
