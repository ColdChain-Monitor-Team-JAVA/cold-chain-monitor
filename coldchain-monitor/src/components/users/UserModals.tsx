import { Modal } from '../Modal'
import { btn } from '../ui'
import type { ManagedUser } from '../../types'
import { UserStatusBadge } from './UserTable'

export function UserDetailsModal({ user, onClose }: { user: ManagedUser; onClose: () => void }) {
  const rows: [string, React.ReactNode][] = [
    ['Full Name', user.name], ['Username', user.username], ['Email', user.email], ['Role', user.role],
    ['Status', <UserStatusBadge key="s" status={user.status} />], ['Last Login', user.lastLogin], ['Account Created', user.createdAt]
  ]
  return (
    <Modal title="User Details" onClose={onClose}>
      <dl className="space-y-3 text-sm">
        {rows.map(([k, v]) => <div key={k} className="flex justify-between gap-4"><dt className="text-slate-500">{k}</dt><dd className="break-all text-right font-medium">{v}</dd></div>)}
      </dl>
      <div className="mt-5 flex justify-end"><button data-autofocus className={btn} onClick={onClose}>Close</button></div>
    </Modal>
  )
}

export function ConfirmModal({ title, text, note, confirmLabel, danger, onConfirm, onClose }: {
  title: string; text: string; note?: string; confirmLabel: string; danger?: boolean; onConfirm: () => void; onClose: () => void
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="text-sm text-slate-700">{text}</p>
      {note && <p className="mt-1 text-sm text-slate-500">{note}</p>}
      <div className="mt-5 flex justify-end gap-2">
        <button data-autofocus className="rounded-md border border-slate-300 px-4 py-2.5 text-sm font-semibold hover:bg-slate-50" onClick={onClose}>Cancel</button>
        <button className={`${btn} ${danger ? '!bg-red-700 hover:!bg-red-800' : ''}`} onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </Modal>
  )
}
