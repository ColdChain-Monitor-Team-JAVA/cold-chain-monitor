import { useEffect, useState } from 'react'
import { Map } from 'lucide-react'
import { getDashboard } from '../services/dashboardService'
import { useAuth } from '../context/AuthContext'
import type { DashboardConfig } from '../types'
import { DataTable, Panel, StatCard, StatusBadge, isStatus } from '../components/ui'

export default function Dashboard({ slug }: { slug: string }) {
  const { user } = useAuth()
  const [data, setData] = useState<DashboardConfig | null>(null)
  useEffect(() => { setData(null); let live = true; getDashboard(slug).then((d) => live && setData(d)); return () => { live = false } }, [slug])

  if (!data) return <p role="status" className="p-6 text-slate-500">Loading dashboard…</p>
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-navy-900">{slug === 'driver' && user ? `Welcome, ${user.name}` : data.title}</h1>
        <p className="text-sm text-slate-500">{data.subtitle}</p>
        <p className="mt-1 text-xs text-slate-400">All values shown are demo data, not live sensor readings.</p>
      </header>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {data.stats.map(([l, v]) => <StatCard key={l} label={l} value={v} />)}
      </div>
      {data.sections?.map((s) => (
        <Panel key={s.title} title={s.title}>
          <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {s.items.map(([k, v]) => (
              <div key={k}><dt className="text-xs uppercase text-slate-500">{k}</dt><dd className="mt-0.5 font-medium">{isStatus(v) ? <StatusBadge status={v} /> : v}</dd></div>
            ))}
          </dl>
        </Panel>
      ))}
      {data.map && (
        <div className="flex h-48 flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-blue-50 text-slate-500">
          <Map className="mb-2 h-8 w-8" aria-hidden="true" />
          <p className="text-sm font-medium">Route map placeholder</p>
          <p className="text-xs">Map integration will be added with the backend.</p>
        </div>
      )}
      <DataTable {...data.table} />
      {data.activity && (
        <Panel title="Recent System Activity">
          <ul className="divide-y divide-slate-100 text-sm">{data.activity.map((a) => <li key={a} className="py-2">{a}</li>)}</ul>
        </Panel>
      )}
    </div>
  )
}
