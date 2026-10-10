import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import { AdminOnly, DashboardRedirect, ProtectedRoute, RoleRoute } from './components/guards'
import { ForgotPasswordPage, LoginPage, MFAPage } from './pages/AuthPages'
import UserManagementPage from './pages/admin/UserManagementPage'
import RolesPage from './pages/admin/RolesPage'
import { NotFound, Placeholder, Profile, Settings } from './pages/Misc'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mfa" element={<MFAPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardRedirect />} />
          <Route path="/dashboard/admin/users" element={<AdminOnly><UserManagementPage /></AdminOnly>} />
          <Route path="/dashboard/admin/roles" element={<AdminOnly><RolesPage /></AdminOnly>} />
          <Route path="/dashboard/admin/activity" element={<AdminOnly><Placeholder title="system activity" /></AdminOnly>} />
          <Route path="/dashboard/:slug" element={<RoleRoute />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/page/:name" element={<Placeholder />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
