export type Role =
  | 'Administrator' | 'Warehouse Manager' | 'Transport Manager' | 'Driver'
  | 'Hospital Staff' | 'Supplier' | 'Quality Inspector'
export interface User { id: string; name: string; username: string; email: string; role: Role }
export interface Section { title: string; items: [string, string][] }
export interface DashboardConfig {
  title: string
  subtitle: string
  stats: [string, string][]
  sections?: Section[]
  map?: boolean
  table: { title: string; columns: string[]; rows: string[][] }
  activity?: string[]
}
export type UserStatus = 'ACTIVE' | 'INACTIVE'
export interface ManagedUser extends User { status: UserStatus; lastLogin: string; createdAt: string }
