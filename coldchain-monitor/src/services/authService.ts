import type { Role, User } from '../types'
import { api, toApiRole, toUser, type ApiUser } from './api'

export interface MfaChallenge { mfaToken: string; expiresInSeconds: number; note: string }

// POST /api/auth/login (the backend decides the real role from the database)
export const loginRequest = (email: string, password: string, role: Role) =>
  api<MfaChallenge>('POST', '/auth/login', { email: email.trim(), password, role: toApiRole(role) })

// POST /api/auth/mfa/verify
export async function verifyMfaRequest(mfaToken: string, code: string): Promise<{ token: string; user: User }> {
  const r = await api<{ token: string; user: ApiUser }>('POST', '/auth/mfa/verify', { mfaToken, code })
  return { token: r.token, user: toUser(r.user) }
}
// POST /api/auth/mfa/resend
export const resendMfaRequest = (mfaToken: string) => api<MfaChallenge>('POST', '/auth/mfa/resend', { mfaToken })
// GET /api/auth/me
export const meRequest = async (): Promise<User> => toUser(await api<ApiUser>('GET', '/auth/me'))
// POST /api/auth/logout
export const logoutRequest = () => api<void>('POST', '/auth/logout')
