import type { ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, Clock, Snowflake, Truck, XOctagon, type LucideIcon } from 'lucide-react'

const OK = 'bg-emerald-50 text-emerald-800 border-emerald-200'
const WARN = 'bg-amber-50 text-amber-800 border-amber-200'
const BAD = 'bg-red-50 text-red-800 border-red-200'
const INFO = 'bg-blue-50 text-blue-800 border-blue-200'
const OFF = 'bg-slate-100 text-slate-700 border-slate-300'
const STYLES: Record<string, [string, LucideIcon]> = {
  NORMAL: [OK, CheckCircle2], WARNING: [WARN, AlertTriangle], CRITICAL: [BAD, XOctagon],
  PENDING: [WARN, Clock], 'IN TRANSIT': [INFO, Truck], DELIVERED: [OK, CheckCircle2],
  COMPLETED: [OK, CheckCircle2], ACTIVE: [OK, CheckCircle2], INACTIVE: [OFF, Clock], OPERATIONAL: [OK, CheckCircle2]
}
export const isStatus = (v: string) => v.toUpperCase() in STYLES

export function StatusBadge({ status }: { status: string }) {
  const [cls, Icon] = STYLES[status.toUpperCase()] ?? [OFF, Clock]
  return (
    <span className={`inline-flex items-center gap-1 rounded border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${cls}`}>
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {status}
    </span>
  )
}

export function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-sm text-slate-500">{label}</p>
      <div className="mt-2 text-2xl font-semibold text-navy-900">{isStatus(value) ? <StatusBadge status={value} /> : value}</div>
    </div>
  )
}

export function DataTable({ title, columns, rows }: { title: string; columns: string[]; rows: string[][] }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm" aria-label={title}>
      <h2 className="border-b border-slate-200 px-4 py-3 text-base font-semibold text-navy-900">{title}</h2>
      {rows.length === 0 ? (
        <p className="p-6 text-center text-sm text-slate-500">No records to display.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-max text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>{columns.map((c) => <th key={c} scope="col" className="px-4 py-2 font-medium">{c}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r, i) => (
                <tr key={i}>
                  {r.map((cell, j) => <td key={j} className="whitespace-nowrap px-4 py-3">{isStatus(cell) ? <StatusBadge status={cell} /> : cell}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export function Logo({ light = false }: { light?: boolean }) {
  return (
    <span className={`flex items-center gap-2 text-lg font-semibold ${light ? 'text-white' : 'text-navy-900'}`}>
      <span className="flex h-8 w-8 items-center justify-center rounded-md bg-blue-600 text-white"><Snowflake className="h-5 w-5" aria-hidden="true" /></span>
      ColdChain Monitor
    </span>
  )
}

export function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="mb-3 text-base font-semibold text-navy-900">{title}</h2>
      {children}
    </section>
  )
}

export const btn = 'inline-flex items-center justify-center rounded-md bg-blue-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-800 disabled:opacity-60'
