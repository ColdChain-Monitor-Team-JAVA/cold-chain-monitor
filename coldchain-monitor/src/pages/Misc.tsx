import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { btn, Panel, StatusBadge } from '../components/ui'

function Message({ title, text, to, cta }: { title: string; text: string; to: string; cta: string }) {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-2xl font-semibold text-navy-900">{title}</h1>
      <p className="mb-6 mt-2 text-slate-500">{text}</p>
      <Link to={to} className={btn}>{cta}</Link>
    </div>
  )
}
export const AccessDenied = () => <Message title="Access Denied" text="You don't have permission to access this page." to="/dashboard" cta="Back to Dashboard" />
export function NotFound() {
  const { isAuthenticated } = useAuth()
  return (
    <div className={isAuthenticated ? '' : 'min-h-screen bg-slate-50 p-6'}>
      <Message title="Page Not Found" text="The page you are looking for does not exist." to={isAuthenticated ? '/dashboard' : '/login'} cta={isAuthenticated ? 'Go to Dashboard' : 'Go to Login'} />
    </div>
  )
}
export function Placeholder({ title }: { title?: string }) {
  const { name = title ?? '' } = useParams()
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold capitalize text-navy-900">{name.replace(/-/g, ' ')}</h1>
      <Panel title="Coming soon"><p className="text-sm text-slate-600">This section will be available once the Spring Boot + PostgreSQL backend is connected.</p></Panel>
    </div>
  )
}
export function Profile() {
  const { user } = useAuth()
  if (!user) return null
  const rows: [string, string][] = [['Name', user.name], ['Username', user.username], ['Email', user.email], ['Role', user.role]]
  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold text-navy-900">Profile</h1>
      <Panel title="Account details">
        <dl className="space-y-3 text-sm">
          {rows.map(([k, v]) => <div key={k} className="flex justify-between gap-4"><dt className="text-slate-500">{k}</dt><dd className="font-medium">{v}</dd></div>)}
          <div className="flex justify-between gap-4"><dt className="text-slate-500">Account status</dt><dd><StatusBadge status="Active" /></dd></div>
        </dl>
      </Panel>
    </div>
  )
}
export function Settings() {
  const row = (id: string, label: string, def = false) => (
    <label key={id} htmlFor={id} className="flex items-center justify-between py-2 text-sm">{label}<input id={id} type="checkbox" defaultChecked={def} className="h-4 w-4" /></label>
  )
  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold text-navy-900">Settings</h1>
      <p className="text-sm text-amber-700">Preview only: these settings are not saved until the backend is connected.</p>
      <Panel title="Account Settings">
        <label htmlFor="lang" className="block text-sm">Language</label>
        <select id="lang" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"><option>English</option></select>
      </Panel>
      <Panel title="Notification Preferences">{[row('n1', 'Email alerts for temperature warnings', true), row('n2', 'Shipment status updates', true), row('n3', 'Weekly summary')]}</Panel>
      <Panel title="Security">{[row('s1', 'Require MFA at sign-in', true), row('s2', 'Sign out after 30 minutes of inactivity')]}</Panel>
    </div>
  )
}
