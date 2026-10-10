import { DASHBOARDS } from '../data/dashboardData'
import type { DashboardConfig } from '../types'

// Later: GET /api/dashboard/{slug}
export async function getDashboard(slug: string): Promise<DashboardConfig | null> {
  await new Promise((r) => setTimeout(r, 250))
  return DASHBOARDS[slug] ?? null
}
