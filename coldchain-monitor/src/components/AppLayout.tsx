import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Bell, ChevronDown, LogOut, Menu, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { NAV, ROLE_SLUG } from '../data/demoUsers'
import { Logo } from './ui'

const slugify = (s: string) => s.toLowerCase().replace(/\s+/g, '-')
const ADMIN_TO: Record<string, string> = { Users: '/dashboard/admin/users', Roles: '/dashboard/admin/roles', 'System Activity': '/dashboard/admin/activity' }

function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { user } = useAuth()
  if (!user) return null
  const items = NAV[user.role].map((label) => ({
    label,
    to: label === 'Dashboard' ? `/dashboard/${ROLE_SLUG[user.role]}` : label === 'Profile' ? '/profile' : label === 'Settings' ? '/settings' : (user.role === 'Administrator' && ADMIN_TO[label]) || `/page/${slugify(label)}`
  }))
  return (
    <div className="flex h-full flex-col bg-navy-900 text-slate-200">
      <div className="px-5 py-5"><Logo light /></div>
      <nav aria-label="Main navigation" className="flex-1 space-y-1 px-3">
        {items.map((i) => (
          <NavLink key={i.to} to={i.to} end onClick={onNavigate}
            className={({ isActive }) => `block rounded-md px-3 py-2 text-sm ${isActive ? 'bg-blue-700 text-white' : 'hover:bg-navy-800'}`}>
            {i.label}
          </NavLink>
        ))}
      </nav>
      <p className="px-5 py-4 text-xs text-slate-400">Demo mode</p>
    </div>
  )
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [drawer, setDrawer] = useState(false)
  const [menu, setMenu] = useState(false)
  if (!user) return null
  const initials = user.name.split(' ').map((p) => p[0]).join('').slice(0, 2)
  const crumb = pathname.split('/').filter(Boolean).map((p) => p.replace(/-/g, ' '))
  const title = crumb[crumb.length - 1] ?? 'dashboard'

  const doLogout = async () => { await logout(); navigate('/login', { replace: true }) }

  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 lg:block"><Sidebar /></aside>
      {drawer && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu">
          <button className="absolute inset-0 bg-black/40" aria-label="Close menu" onClick={() => setDrawer(false)} />
          <div className="relative h-full w-64">
            <Sidebar onNavigate={() => setDrawer(false)} />
            <button className="absolute right-2 top-4 text-white" aria-label="Close menu" onClick={() => setDrawer(false)}><X /></button>
          </div>
        </div>
      )}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-3">
            <button className="rounded p-1 lg:hidden" aria-label="Open menu" onClick={() => setDrawer(true)}><Menu /></button>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold capitalize text-navy-900">{title}</p>
              <nav aria-label="Breadcrumb" className="hidden truncate text-xs capitalize text-slate-500 sm:block">Home / {crumb.join(' / ')}</nav>
            </div>
          </div>
          <div className="flex items-center gap-2 sm:gap-4">
            <button className="rounded p-2 text-slate-600 hover:bg-slate-100" aria-label="Notifications"><Bell className="h-5 w-5" /></button>
            <div className="relative">
              <button className="flex items-center gap-2 rounded-md p-1 hover:bg-slate-100" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)}>
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-700 text-sm font-semibold text-white">{initials}</span>
                <span className="hidden text-left sm:block"><span className="block text-sm font-medium">{user.name}</span><span className="block text-xs text-slate-500">{user.role}</span></span>
                <ChevronDown className="h-4 w-4" aria-hidden="true" />
              </button>
              {menu && (
                <>
                  <button className="fixed inset-0 z-10 cursor-default" aria-label="Close user menu" onClick={() => setMenu(false)} />
                  <div role="menu" className="absolute right-0 z-20 mt-2 w-44 rounded-md border border-slate-200 bg-white py-1 text-sm shadow-lg">
                    <Link role="menuitem" className="block px-4 py-2 hover:bg-slate-50" to="/profile" onClick={() => setMenu(false)}>Profile</Link>
                    <Link role="menuitem" className="block px-4 py-2 hover:bg-slate-50" to="/settings" onClick={() => setMenu(false)}>Settings</Link>
                    <button role="menuitem" className="flex w-full items-center gap-2 px-4 py-2 text-left hover:bg-slate-50" onClick={doLogout}><LogOut className="h-4 w-4" aria-hidden="true" />Logout</button>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>
        <main className="mx-auto max-w-7xl p-4 sm:p-6"><Outlet /></main>
      </div>
    </div>
  )
}
