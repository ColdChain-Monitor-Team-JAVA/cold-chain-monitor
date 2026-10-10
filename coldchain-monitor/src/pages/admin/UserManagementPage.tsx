import { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { ROLES } from '../../data/demoUsers'
import { createUser, deleteUser, getUsers, updateUser, updateUserStatus, type UserInput } from '../../services/userService'
import type { ManagedUser } from '../../types'
import { btn } from '../../components/ui'
import { Toast } from '../../components/Modal'
import { UserTable } from '../../components/users/UserTable'
import { UserFormModal } from '../../components/users/UserFormModal'
import { ConfirmModal, UserDetailsModal } from '../../components/users/UserModals'

type ModalState = { type: 'add' } | { type: 'view' | 'edit' | 'delete' | 'deactivate'; id: string } | null
const sel = 'rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm'

export default function UserManagementPage() {
  const { user: me } = useAuth()
  const meId = me?.id ?? ''
  const [users, setUsers] = useState<ManagedUser[] | null>(null)
  const [loadError, setLoadError] = useState('')
  const [q, setQ] = useState('')
  const [role, setRole] = useState('')
  const [status, setStatus] = useState('')
  const [modal, setModal] = useState<ModalState>(null)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const closeToast = useCallback(() => setToast(null), [])

  const reload = async () => setUsers(await getUsers())
  useEffect(() => { getUsers().then(setUsers).catch(() => setLoadError('Unable to load users. Please refresh the page.')) }, [])

  const list = users ?? []
  const stats: [string, number][] = [
    ['Total Users', list.length],
    ['Active Users', list.filter((u) => u.status === 'ACTIVE').length],
    ['Inactive Users', list.filter((u) => u.status === 'INACTIVE').length],
    ['Administrators', list.filter((u) => u.role === 'Administrator').length]
  ]
  const filtered = useMemo(() => {
    const t = q.trim().toLowerCase()
    return list.filter((u) =>
      (!role || u.role === role) && (!status || u.status === status) &&
      (!t || [u.name, u.username, u.email, u.role].some((f) => f.toLowerCase().includes(t))))
  }, [list, q, role, status])

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    try { await fn(); await reload(); setModal(null); setToast({ message: ok, type: 'success' }) }
    catch (e) { setModal(null); setToast({ message: e instanceof Error ? e.message : 'Action failed.', type: 'error' }) }
  }
  const fail = (message: string) => setToast({ message, type: 'error' })
  const onDelete = (u: ManagedUser) => (u.id === meId ? fail('You cannot delete the account you are currently using.') : setModal({ type: 'delete', id: u.id }))
  const onToggle = (u: ManagedUser) => {
    if (u.status === 'INACTIVE') return void run(() => updateUserStatus(u.id, 'ACTIVE'), 'User activated successfully.')
    if (u.id === meId) return fail('You cannot deactivate the account you are currently using.')
    setModal({ type: 'deactivate', id: u.id })
  }
  const clear = () => { setQ(''); setRole(''); setStatus('') }
  const target = modal && modal.type !== 'add' ? list.find((u) => u.id === modal.id) : undefined
  const close = () => setModal(null)

  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-navy-900">User Management</h1>
          <p className="text-sm text-slate-500">Create, manage, and control user accounts and system roles.</p>
        </div>
        <button className={`${btn} gap-1`} onClick={() => setModal({ type: 'add' })}><Plus className="h-4 w-4" aria-hidden="true" />Add User</button>
      </header>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map(([l, v]) => (
          <div key={l} className="rounded-lg border border-slate-200 bg-white px-4 py-3"><p className="text-xs text-slate-500">{l}</p><p className="text-xl font-semibold text-navy-900">{v}</p></div>
        ))}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <label htmlFor="user-search" className="sr-only">Search users</label>
          <Search className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-slate-400" aria-hidden="true" />
          <input id="user-search" type="search" placeholder="Search users..." className={`${sel} w-full pl-9`} value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <label className="sr-only" htmlFor="f-role">Filter by role</label>
        <select id="f-role" className={sel} value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="">All Roles</option>{ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
        <label className="sr-only" htmlFor="f-status">Filter by status</label>
        <select id="f-status" className={sel} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All Statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
        </select>
      </div>

      {loadError ? <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">{loadError}</p>
        : !users ? <p role="status" className="text-slate-500">Loading users…</p>
        : filtered.length === 0 ? (
          <div className="rounded-lg border border-slate-200 bg-white p-10 text-center">
            <h2 className="text-lg font-semibold text-navy-900">No users found</h2>
            <p className="mb-4 mt-1 text-sm text-slate-500">Try changing your search or filter criteria.</p>
            <button className={btn} onClick={clear}>Clear Filters</button>
          </div>
        ) : <UserTable users={filtered} onView={(u) => setModal({ type: 'view', id: u.id })} onEdit={(u) => setModal({ type: 'edit', id: u.id })} onToggle={onToggle} onDelete={onDelete} />}

      {modal?.type === 'add' && (
        <UserFormModal users={list} currentId={meId} onClose={close}
          onSubmit={async (v: UserInput, pw) => { await createUser({ ...v, password: pw ?? '' }); await reload(); close(); setToast({ message: 'User created successfully.', type: 'success' }) }} />
      )}
      {modal?.type === 'edit' && target && (
        <UserFormModal initial={target} users={list} currentId={meId} onClose={close}
          onSubmit={async (v) => { await updateUser(target.id, v); await reload(); close(); setToast({ message: 'User updated successfully.', type: 'success' }) }} />
      )}
      {modal?.type === 'view' && target && <UserDetailsModal user={target} onClose={close} />}
      {modal?.type === 'delete' && target && (
        <ConfirmModal danger title="Delete User?" text="Are you sure you want to delete this user account?" note="This action cannot be undone." confirmLabel="Delete User" onClose={close}
          onConfirm={() => run(() => deleteUser(target.id), 'User deleted successfully.')} />
      )}
      {modal?.type === 'deactivate' && target && (
        <ConfirmModal title="Deactivate User?" text="This user will no longer be able to sign in." confirmLabel="Deactivate" onClose={close}
          onConfirm={() => run(() => updateUserStatus(target.id, 'INACTIVE'), 'User deactivated successfully.')} />
      )}
      {toast && <Toast message={toast.message} type={toast.type} onDone={closeToast} />}
    </div>
  )
}
