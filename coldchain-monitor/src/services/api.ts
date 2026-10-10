import { ROLES } from '../data/demoUsers'
import type { ManagedUser, Role, User, UserStatus } from '../types'

const BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
const TOKEN = 'coldchain_token'
const USER = 'coldchain_user'
const AUTH = 'coldchain_auth'

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}
export const errMsg = (e: unknown) => (e instanceof Error ? e.message : 'Something went wrong.')

export const toApiRole = (r: Role) => r.toUpperCase().replace(/ /g, '_')
function fromApiRole(s: string): Role {
  const r = ROLES.find((x) => toApiRole(x) === s)
  if (!r) throw new Error(`Unknown role: ${s}`)
  return r
}
export interface ApiUser { id: number; name: string; username: string; email: string; role: string; status: UserStatus; lastLogin: string | null; createdAt: string }
export const toUser = (u: ApiUser): User => ({ id: String(u.id), name: u.name, username: u.username, email: u.email, role: fromApiRole(u.role) })
export const toManaged = (u: ApiUser): ManagedUser => ({
  ...toUser(u), status: u.status,
  lastLogin: u.lastLogin ? new Date(u.lastLogin).toLocaleString() : 'Never',
  createdAt: u.createdAt.slice(0, 10)
})

// Session storage: localStorage when "Remember me" is ticked, otherwise sessionStorage. Never a password.
export function getToken(): string | null {
  try { return localStorage.getItem(TOKEN) ?? sessionStorage.getItem(TOKEN) } catch { return null }
}
export function clearSession(): void {
  for (const s of [localStorage, sessionStorage]) { try { [TOKEN, USER, AUTH].forEach((k) => s.removeItem(k)) } catch { /* ignore */ } }
}
export function saveSession(token: string, user: User, remember: boolean): void {
  clearSession()
  const s = remember ? localStorage : sessionStorage
  s.setItem(TOKEN, token); s.setItem(USER, JSON.stringify(user)); s.setItem(AUTH, 'true')
}
export function loadSession(): User | null {
  try {
    for (const s of [localStorage, sessionStorage]) {
      const u = s.getItem(USER)
      if (u && s.getItem(TOKEN)) return JSON.parse(u) as User
    }
  } catch { /* ignore */ }
  return null
}

export async function api<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = getToken()
  let res: Response
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch { throw new ApiError(0, 'Unable to reach the server. Is the backend running on http://localhost:8080?') }
  if (res.status === 204) return undefined as T
  const data = (await res.json().catch(() => null)) as { message?: string } | null
  if (!res.ok) throw new ApiError(res.status, data?.message ?? `Request failed (${res.status}).`)
  return data as T
}
