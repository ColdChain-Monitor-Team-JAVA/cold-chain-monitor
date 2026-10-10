import type { ManagedUser, Role } from '../types'

// DEMO ONLY - never use real credentials in frontend code.
export const MFA_DEMO_CODE = '123456'
export const ROLE_SLUG: Record<Role, string> = {
  Administrator: 'admin', 'Warehouse Manager': 'warehouse', 'Transport Manager': 'transport',
  Driver: 'driver', 'Hospital Staff': 'hospital', Supplier: 'supplier', 'Quality Inspector': 'inspector'
}
export const NAV: Record<Role, string[]> = {
  Administrator: ['Dashboard', 'Users', 'Roles', 'System Activity', 'Profile', 'Settings'],
  'Warehouse Manager': ['Dashboard', 'Storage', 'Inventory', 'Shipments', 'Profile', 'Settings'],
  'Transport Manager': ['Dashboard', 'Shipments', 'Active Transports', 'Routes', 'Profile', 'Settings'],
  Driver: ['Dashboard', 'My Deliveries', 'Active Shipment', 'Route', 'Profile', 'Settings'],
  'Hospital Staff': ['Dashboard', 'Incoming Shipments', 'Received Goods', 'Storage', 'Profile', 'Settings'],
  Supplier: ['Dashboard', 'My Shipments', 'Products', 'Delivery Status', 'Profile', 'Settings'],
  'Quality Inspector': ['Dashboard', 'Quality Checks', 'Temperature Records', 'Inspection History', 'Profile', 'Settings']
}
export const ROLES = Object.keys(ROLE_SLUG) as Role[]
const mk = (username: string, name: string, role: Role, lastLogin: string, createdAt: string): ManagedUser =>
  ({ id: `u-${username}`, name, username, email: `${username}@coldchain.demo`, role, status: 'ACTIVE', lastLogin, createdAt })
export const DEMO_USERS: ManagedUser[] = [
  mk('admin', 'Vikram Nair', 'Administrator', 'Just now', '2026-01-10'),
  mk('warehouse', 'Rahul Sharma', 'Warehouse Manager', '10 min ago', '2026-01-15'),
  mk('transport', 'Ananya Rao', 'Transport Manager', '25 min ago', '2026-01-15'),
  mk('driver', 'Kiran Kumar', 'Driver', '1 hour ago', '2026-02-02'),
  mk('hospital', 'Priya Singh', 'Hospital Staff', '2 hours ago', '2026-02-11'),
  mk('supplier', 'Arjun Mehta', 'Supplier', 'Yesterday', '2026-03-01'),
  mk('inspector', 'Sneha Iyer', 'Quality Inspector', 'Yesterday', '2026-03-09')
]
