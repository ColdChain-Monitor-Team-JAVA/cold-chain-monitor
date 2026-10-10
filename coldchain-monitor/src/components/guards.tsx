import type { ReactNode } from 'react'
import { Navigate, Outlet, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ROLE_SLUG } from '../data/demoUsers'
import { DASHBOARDS } from '../data/dashboardData'
import Dashboard from '../pages/Dashboard'
import { AccessDenied, NotFound } from '../pages/Misc'

export function ProtectedRoute() {
  const { isAuthenticated, isMfaVerified } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!isMfaVerified) return <Navigate to="/mfa" replace />
  return <Outlet />
}

export function RoleRoute() {
  const { slug = '' } = useParams()
  const { user } = useAuth()
  if (!(slug in DASHBOARDS)) return <NotFound />
  if (!user || ROLE_SLUG[user.role] !== slug) return <AccessDenied />
  return <Dashboard slug={slug} />
}

export function DashboardRedirect() {
  const { user } = useAuth()
  return user ? <Navigate to={`/dashboard/${ROLE_SLUG[user.role]}`} replace /> : <Navigate to="/login" replace />
}

export function AdminOnly({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return user?.role === 'Administrator' ? <>{children}</> : <AccessDenied />
}
