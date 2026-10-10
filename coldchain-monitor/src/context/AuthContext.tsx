import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { Role, User } from '../types'
import { loginRequest, logoutRequest, meRequest, resendMfaRequest, verifyMfaRequest } from '../services/authService'
import { ApiError, clearSession, errMsg, loadSession, saveSession } from '../services/api'

const PENDING_KEY = 'coldchain_mfa' // sessionStorage: MFA challenge token only (never a password)
interface Pending { token: string; remember: boolean }

interface AuthValue {
  user: User | null
  isAuthenticated: boolean
  isMfaVerified: boolean
  mfaPending: boolean
  login: (email: string, password: string, role: Role, remember: boolean) => Promise<string | null>
  verifyMFA: (code: string) => Promise<string | null>
  resendMFA: () => Promise<string | null>
  logout: () => Promise<void>
}
const AuthContext = createContext<AuthValue | null>(null)

function readPending(): Pending | null {
  try { const v = sessionStorage.getItem(PENDING_KEY); return v ? (JSON.parse(v) as Pending) : null } catch { return null }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadSession)
  const [pending, setPending] = useState<Pending | null>(readPending)

  // Re-validate a stored session with the backend (the database role is authoritative)
  useEffect(() => {
    if (!loadSession()) return
    meRequest().then(setUser).catch((e) => { if (e instanceof ApiError && e.status === 401) { clearSession(); setUser(null) } })
  }, [])

  const login = useCallback(async (email: string, password: string, role: Role, remember: boolean) => {
    try {
      const c = await loginRequest(email, password, role)
      const p = { token: c.mfaToken, remember }
      sessionStorage.setItem(PENDING_KEY, JSON.stringify(p))
      setPending(p)
      return null
    } catch (e) { return errMsg(e) }
  }, [])

  const verifyMFA = useCallback(async (code: string) => {
    if (!pending) return 'Your sign-in session has expired. Please sign in again.'
    try {
      const r = await verifyMfaRequest(pending.token, code)
      saveSession(r.token, r.user, pending.remember)
      sessionStorage.removeItem(PENDING_KEY)
      setPending(null)
      setUser(r.user)
      return null
    } catch (e) { return errMsg(e) }
  }, [pending])

  const resendMFA = useCallback(async () => {
    if (!pending) return 'Your sign-in session has expired. Please sign in again.'
    try { await resendMfaRequest(pending.token); return null } catch (e) { return errMsg(e) }
  }, [pending])

  const logout = useCallback(async () => {
    try { await logoutRequest() } catch { /* token may already be invalid */ }
    clearSession()
    sessionStorage.removeItem(PENDING_KEY)
    setPending(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthValue>(
    () => ({ user, isAuthenticated: !!user, isMfaVerified: !!user, mfaPending: !!pending, login, verifyMFA, resendMFA, logout }),
    [user, pending, login, verifyMFA, resendMFA, logout]
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
