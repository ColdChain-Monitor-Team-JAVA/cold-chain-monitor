import { useState } from 'react'
import { MoreVertical } from 'lucide-react'
import { StatusBadge } from '../ui'
import type { ManagedUser, UserStatus } from '../../types'

export const UserStatusBadge = ({ status }: { status: UserStatus }) => <StatusBadge status={status === 'ACTIVE' ? 'Active' : 'Inactive'} />

export const initials = (name: string) => name.split(/\s+/).filter(Boolean).map((p) => p[0]).join('').slice(0, 2).toUpperCase()
const Avatar = ({ name }: { name: string }) => (
  <span aria-hidden="true" className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-800">{initials(name)}</span>
)

interface Handlers { onView: (u: ManagedUser) => void; onEdit: (u: ManagedUser) => void; onToggle: (u: ManagedUser) => void; onDelete: (u: ManagedUser) => void }

function RowMenu({ u, ...h }: { u: ManagedUser } & Handlers) {
  const [open, setOpen] = useState(false)
  const act = (f: (u: ManagedUser) => void) => () => { setOpen(false); f(u) }
  const item = 'block w-full px-4 py-2 text-left hover:bg-slate-50'
  return (
    <div className="relative">
      <button className="rounded p-1.5 hover:bg-slate-100" aria-label={`Actions for ${u.name}`} aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen(!open)}>
        <MoreVertical className="h-4 w-4" aria-hidden="true" />
      </button>
      {open && (
        <>
          <button className="fixed inset-0 z-10 cursor-default" aria-label="Close actions menu" tabIndex={-1} onClick={() => setOpen(false)} />
          <div role="menu" className="absolute right-0 z-20 mt-1 w-44 rounded-md border border-slate-200 bg-white py-1 text-sm shadow-lg">
            <button role="menuitem" className={item} onClick={act(h.onView)}>View</button>
            <button role="menuitem" className={item} onClick={act(h.onEdit)}>Edit</button>
            <button role="menuitem" className={item} onClick={act(h.onToggle)}>{u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
            <button role="menuitem" className={`${item} text-red-700`} onClick={act(h.onDelete)}>Delete</button>
          </div>
        </>
      )}
    </div>
  )
}

export function UserTable({ users, ...h }: { users: ManagedUser[] } & Handlers) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <table className="hidden w-full text-left text-sm xl:table">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr>{['User', 'Username', 'Email', 'Role', 'Status', 'Last Login'].map((c) => <th key={c} scope="col" className="px-4 py-3 font-medium">{c}</th>)}<th scope="col" className="px-4 py-3 text-right font-medium">Actions</th></tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {users.map((u) => (
            <tr key={u.id}>
              <td className="px-4 py-3"><div className="flex items-center gap-3"><Avatar name={u.name} /><span className="font-medium">{u.name}</span></div></td>
              <td className="px-4 py-3">{u.username}</td>
              <td className="break-all px-4 py-3">{u.email}</td>
              <td className="px-4 py-3">{u.role}</td>
              <td className="px-4 py-3"><UserStatusBadge status={u.status} /></td>
              <td className="whitespace-nowrap px-4 py-3">{u.lastLogin}</td>
              <td className="px-4 py-3"><div className="flex justify-end"><RowMenu u={u} {...h} /></div></td>
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="divide-y divide-slate-100 xl:hidden">
        {users.map((u) => (
          <li key={u.id} className="flex items-start justify-between gap-3 p-4">
            <div className="flex min-w-0 gap-3">
              <Avatar name={u.name} />
              <div className="min-w-0 text-sm">
                <p className="font-medium">{u.name} <span className="font-normal text-slate-500">@{u.username}</span></p>
                <p className="break-all text-slate-500">{u.email}</p>
                <p className="mt-1">{u.role} · Last login: {u.lastLogin}</p>
                <div className="mt-2"><UserStatusBadge status={u.status} /></div>
              </div>
            </div>
            <RowMenu u={u} {...h} />
          </li>
        ))}
      </ul>
    </div>
  )
}
