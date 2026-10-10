import { useEffect, useRef, useState, type ClipboardEvent, type FormEvent, type KeyboardEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { btn, Logo } from '../components/ui'
import { ROLES } from '../data/demoUsers'
import type { Role } from '../types'

function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="hidden flex-col justify-between bg-navy-900 p-12 text-white lg:flex">
        <Logo light />
        <div>
          <svg viewBox="0 0 320 140" className="mb-8 w-full max-w-sm" role="img" aria-label="Refrigerated truck illustration">
            <rect x="10" y="30" width="190" height="80" rx="6" fill="#1e3a6e" stroke="#60a5fa" strokeWidth="2" />
            <path d="M200 55h60l30 30v25H200z" fill="#2563eb" />
            <circle cx="60" cy="115" r="14" fill="#0b1a33" stroke="#93c5fd" strokeWidth="3" />
            <circle cx="240" cy="115" r="14" fill="#0b1a33" stroke="#93c5fd" strokeWidth="3" />
            <path d="M105 50v40M85 60l40 20M85 80l40-20" stroke="#bfdbfe" strokeWidth="3" strokeLinecap="round" />
            <text x="150" y="20" fill="#93c5fd" fontSize="14" textAnchor="middle">2–8 °C</text>
          </svg>
          <h1 className="text-3xl font-semibold">Reliable monitoring for temperature-sensitive shipments</h1>
          <p className="mt-3 max-w-md text-slate-300">Monitor temperature, humidity, storage conditions, and shipment status in real time.</p>
        </div>
        <p className="text-xs text-slate-400">Demo environment</p>
      </div>
      <div className="flex items-center justify-center p-4 sm:p-8">{children}</div>
    </div>
  )
}

const inputCls = 'w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm'

export function LoginPage() {
  const { login, isAuthenticated, isMfaVerified } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role | ''>('')
  const [remember, setRemember] = useState(false)
  const [show, setShow] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (isAuthenticated && isMfaVerified) return <Navigate to="/dashboard" replace />

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const em = email.trim()
    if (!em) return setError('Email is required.')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(em)) return setError('Enter a valid email address.')
    if (!password) return setError('Password is required.')
    if (!role) return setError('Please select a role.')
    setBusy(true); setError('')
    const err = await login(em, password, role, remember)
    setBusy(false)
    if (err) setError(err)
    else navigate('/mfa')
  }
  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 className="text-2xl font-semibold text-navy-900">Welcome back</h2>
        <p className="mb-6 text-sm text-slate-500">Sign in to your ColdChain Monitor account</p>
        <form onSubmit={submit} noValidate className="space-y-4">
          {error && <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>}
          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium">Email</label>
            <input id="email" type="email" className={inputCls} autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium">Password</label>
            <div className="relative">
              <input id="password" type={show ? 'text' : 'password'} className={`${inputCls} pr-10`} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
              <button type="button" className="absolute inset-y-0 right-0 px-3 text-slate-500" aria-label={show ? 'Hide password' : 'Show password'} onClick={() => setShow(!show)}>
                {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>
          <div>
            <label htmlFor="role" className="mb-1 block text-sm font-medium">Role</label>
            <select id="role" className={inputCls} value={role} onChange={(e) => setRole(e.target.value as Role | '')}>
              <option value="">Select your role</option>
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2"><input type="checkbox" className="h-4 w-4" checked={remember} onChange={(e) => setRemember(e.target.checked)} />Remember me</label>
            <Link to="/forgot-password" className="text-blue-700 hover:underline">Forgot password?</Link>
          </div>
          <button type="submit" disabled={busy} className={`${btn} w-full`}>{busy ? 'Signing in…' : 'Sign In'}</button>
        </form>
        <div className="mt-6 rounded-md bg-slate-50 p-3 text-xs text-slate-600">
          <p className="font-semibold">Demo accounts (development only)</p>
          <p>e.g. admin@coldchain.local, driver@coldchain.local, inspector@coldchain.local</p>
          <p>Password: Admin@123 &nbsp;|&nbsp; Demo MFA code: 123456</p>
        </div>
      </div>
    </AuthLayout>
  )
}

export function MFAPage() {
  const { verifyMFA, resendMFA, mfaPending, isMfaVerified, logout } = useAuth()
  const navigate = useNavigate()
  const [digits, setDigits] = useState<string[]>(Array(6).fill(''))
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [seconds, setSeconds] = useState(30)
  const refs = useRef<(HTMLInputElement | null)[]>([])

  useEffect(() => { if (seconds <= 0) return; const t = setTimeout(() => setSeconds((s) => s - 1), 1000); return () => clearTimeout(t) }, [seconds])
  useEffect(() => { refs.current[0]?.focus() }, [])

  if (isMfaVerified) return <Navigate to="/dashboard" replace />
  if (!mfaPending) return <Navigate to="/login" replace />

  const setAt = (i: number, v: string) => setDigits((d) => d.map((x, k) => (k === i ? v : x)))
  const onChange = (i: number, v: string) => {
    const c = v.replace(/\D/g, '').slice(-1)
    setAt(i, c)
    if (c && i < 5) refs.current[i + 1]?.focus()
  }
  const onKey = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[i] && i > 0) refs.current[i - 1]?.focus()
    if (e.key === 'ArrowLeft' && i > 0) refs.current[i - 1]?.focus()
    if (e.key === 'ArrowRight' && i < 5) refs.current[i + 1]?.focus()
  }
  const onPaste = (e: ClipboardEvent) => {
    const t = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!t) return
    e.preventDefault()
    setDigits(Array.from({ length: 6 }, (_, i) => t[i] ?? ''))
    refs.current[Math.min(t.length, 5)]?.focus()
  }
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setError('')
    const err = await verifyMFA(digits.join(''))
    setBusy(false)
    if (err) setError(err)
    else navigate('/dashboard', { replace: true })
  }
  const resend = async () => { const err = await resendMFA(); if (err) { setError(err); return } setSeconds(30); setDigits(Array(6).fill('')); setError(''); refs.current[0]?.focus() }
  const back = async () => { await logout(); navigate('/login') }

  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 className="text-2xl font-semibold text-navy-900">Verify your identity</h2>
        <p className="mb-1 mt-1 text-sm text-slate-500">Enter the verification code sent to your registered authentication method.</p>
        <p className="mb-6 text-xs text-amber-700">Development/demo MFA only (not email/SMS): the code is 123456.</p>
        <form onSubmit={submit} className="space-y-4">
          {error && <p id="mfa-error" role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>}
          <div className="flex justify-between gap-2" role="group" aria-label="Six digit verification code">
            {digits.map((d, i) => (
              <input key={i} ref={(el) => { refs.current[i] = el }} value={d} inputMode="numeric" autoComplete={i === 0 ? 'one-time-code' : 'off'}
                maxLength={1} aria-label={`Digit ${i + 1}`} aria-describedby={error ? 'mfa-error' : undefined}
                className="h-12 w-full min-w-0 rounded-md border border-slate-300 text-center text-lg font-semibold"
                onChange={(e) => onChange(i, e.target.value)} onKeyDown={(e) => onKey(i, e)} onPaste={onPaste} />
            ))}
          </div>
          <button type="submit" disabled={busy || digits.some((d) => !d)} className={`${btn} w-full`}>{busy ? 'Verifying…' : 'Verify & Continue'}</button>
        </form>
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2 text-sm">
          <button className="text-blue-700 hover:underline disabled:text-slate-400 disabled:no-underline" disabled={seconds > 0} onClick={resend}>
            {seconds > 0 ? `Resend code in ${seconds}s` : 'Resend code'}
          </button>
          <button className="text-blue-700 hover:underline" onClick={() => setError('Only the authenticator code is available in demo mode.')}>Use another verification method</button>
        </div>
        <button className="mt-4 text-sm text-slate-600 hover:underline" onClick={back}>Back to Sign In</button>
      </div>
    </AuthLayout>
  )
}

export function ForgotPasswordPage() {
  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 className="text-2xl font-semibold text-navy-900">Forgot password</h2>
        <p className="mb-6 mt-2 text-sm text-slate-600">Password reset functionality will be configured with email service in the production version.</p>
        <Link to="/login" className={btn}>Back to Login</Link>
      </div>
    </AuthLayout>
  )
}
