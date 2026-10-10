import { useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Modal } from '../Modal'
import { btn } from '../ui'
import { ROLES } from '../../data/demoUsers'
import type { ManagedUser, Role, UserStatus } from '../../types'
import type { UserInput } from '../../services/userService'

const cls = 'w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm'

interface Props {
  initial?: ManagedUser
  users: ManagedUser[]
  currentId: string
  onSubmit: (v: UserInput, password?: string) => Promise<void>
  onClose: () => void
}

export function UserFormModal({ initial, users, currentId, onSubmit, onClose }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [username, setUsername] = useState(initial?.username ?? '')
  const [email, setEmail] = useState(initial?.email ?? '')
  const [role, setRole] = useState<Role | ''>(initial?.role ?? '')
  const [status, setStatus] = useState<UserStatus>(initial?.status ?? 'ACTIVE')
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [resetMsg, setResetMsg] = useState(false)
  const [busy, setBusy] = useState(false)

  const ip = (k: string) => ({ id: k, 'aria-invalid': !!errors[k], 'aria-describedby': errors[k] ? `${k}-err` : undefined, className: `${cls} ${errors[k] ? 'border-red-500' : ''}` })
  const Err = ({ k }: { k: string }) => (errors[k] ? <p id={`${k}-err`} className="mt-1 text-xs text-red-700">{errors[k]}</p> : null)
  const Label = ({ k, children }: { k: string; children: string }) => <label htmlFor={k} className="mb-1 block text-sm font-medium">{children}</label>

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Record<string, string> = {}
    const un = username.trim().toLowerCase()
    const em = email.trim().toLowerCase()
    if (!name.trim()) errs.name = 'Full name is required.'
    if (!un) errs.username = 'Username is required.'
    else if (/\s/.test(un)) errs.username = 'Username cannot contain spaces.'
    else if (users.some((u) => u.id !== initial?.id && u.username.toLowerCase() === un)) errs.username = 'This username is already in use.'
    if (!em) errs.email = 'Email is required.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(em)) errs.email = 'Enter a valid email address.'
    else if (users.some((u) => u.id !== initial?.id && u.email.toLowerCase() === em)) errs.email = 'This email is already associated with another account.'
    if (!role) errs.role = 'Role is required.'
    if (!initial && !password) errs.password = 'Temporary password is required.'
    if (initial && initial.id === currentId) {
      if (status === 'INACTIVE') errs.status = 'You cannot deactivate the account you are currently using.'
      if (role && role !== initial.role) errs.role = 'You cannot change the role of the account you are currently using.'
    }
    setErrors(errs)
    if (Object.keys(errs).length > 0 || !role) return
    setBusy(true); setFormError('')
    try { await onSubmit({ name: name.trim(), username: un, email: em, role, status }, initial ? undefined : password) }
    catch (err) { setFormError(err instanceof Error ? err.message : 'Something went wrong.'); setBusy(false) }
  }

  return (
    <Modal title={initial ? 'Edit User' : 'Add User'} onClose={onClose}>
      <form onSubmit={submit} noValidate className="space-y-4">
        {formError && <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{formError}</p>}
        <div><Label k="name">Full Name</Label><input {...ip('name')} value={name} onChange={(e) => setName(e.target.value)} /><Err k="name" /></div>
        <div><Label k="username">Username</Label><input {...ip('username')} autoComplete="off" value={username} onChange={(e) => setUsername(e.target.value)} /><Err k="username" /></div>
        <div><Label k="email">Email</Label><input {...ip('email')} type="email" autoComplete="off" value={email} onChange={(e) => setEmail(e.target.value)} /><Err k="email" /></div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label k="role">Role</Label>
            <select {...ip('role')} value={role} onChange={(e) => setRole(e.target.value as Role | '')}>
              <option value="">Select a role</option>
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <Err k="role" />
          </div>
          <div>
            <Label k="status">Status</Label>
            <select {...ip('status')} value={status} onChange={(e) => setStatus(e.target.value as UserStatus)}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
            <Err k="status" />
          </div>
        </div>
        {!initial ? (
          <div>
            <Label k="password">Temporary Password</Label>
            <div className="relative">
              <input {...ip('password')} type={show ? 'text' : 'password'} autoComplete="new-password" className={`${cls} pr-10 ${errors.password ? 'border-red-500' : ''}`} value={password} onChange={(e) => setPassword(e.target.value)} />
              <button type="button" className="absolute inset-y-0 right-0 px-3 text-slate-500" aria-label={show ? 'Hide password' : 'Show password'} onClick={() => setShow(!show)}>
                {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            <Err k="password" />
            <p className="mt-1 text-xs text-slate-500">Demo mode only: used by the mock sign-in, stored as a hash, not real password security.</p>
          </div>
        ) : (
          <div>
            <button type="button" className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-50" onClick={() => setResetMsg(true)}>Reset Password</button>
            {resetMsg && <p role="status" className="mt-2 text-xs text-slate-600">Password reset will be available when the backend authentication service is connected.</p>}
          </div>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="rounded-md border border-slate-300 px-4 py-2.5 text-sm font-semibold hover:bg-slate-50" onClick={onClose}>Cancel</button>
          <button type="submit" disabled={busy} className={btn}>{busy ? 'Saving…' : initial ? 'Save Changes' : 'Create User'}</button>
        </div>
      </form>
    </Modal>
  )
}
