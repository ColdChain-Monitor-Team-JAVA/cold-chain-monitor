import type { DashboardConfig } from '../types'
import { activity } from './activityData'
import { admin, warehouse, driver, hospital, supplier, inspector } from './monitoringData'
import { shipments } from './shipmentData'

// All values below are DEMO DATA, not real sensor readings.
export const DASHBOARDS: Record<string, DashboardConfig> = {
  admin: {
    title: 'Administrator Dashboard',
    subtitle: 'Overview of ColdChain Monitor system activity and users.',
    stats: [['Total Users', '24'], ['Active Users', '21'], ['Inactive Users', '3'], ['System Status', 'Operational']],
    table: admin.users,
    sections: [{ title: 'Cold-Chain Monitoring Overview', items: admin.overview }],
    activity
  },
  warehouse: {
    title: 'Warehouse Dashboard', subtitle: 'Storage conditions and incoming stock (demo data).',
    stats: [['Storage Units', '12'], ['Normal Conditions', '10'], ['Attention Required', '2'], ['Incoming Shipments', '5']],
    table: warehouse
  },
  transport: {
    title: 'Transport Dashboard', subtitle: 'Vehicles and shipments in transit (demo data).',
    stats: [['Active Shipments', '8'], ['Vehicles In Transit', '5'], ['Completed Deliveries', '12'], ['Attention Required', '1']],
    table: { title: 'Shipments', columns: ['Shipment ID', 'Origin', 'Destination', 'Vehicle', 'Temperature', 'Status'], rows: shipments.transport }
  },
  driver: {
    title: 'Driver Dashboard', subtitle: 'Your assigned deliveries (demo data).',
    stats: [['Assigned Deliveries', '4'], ['Active Delivery', '1'], ['Completed Deliveries', '12']],
    sections: [{ title: 'Current Delivery', items: driver.current }],
    map: true,
    table: driver.table
  },
  hospital: {
    title: 'Hospital Dashboard', subtitle: 'Incoming shipments and storage status (demo data).',
    stats: [['Incoming Shipments', '6'], ['Received Shipments', '19'], ['Storage Status', 'Normal'], ['Temperature Alerts', '1']],
    table: hospital
  },
  supplier: {
    title: 'Supplier Dashboard', subtitle: 'Your shipments and deliveries (demo data).',
    stats: [['Total Shipments', '34'], ['Active Shipments', '7'], ['Delivered Shipments', '24'], ['Pending Deliveries', '3']],
    table: supplier
  },
  inspector: {
    title: 'Quality Inspector Dashboard', subtitle: 'Inspection progress and deviations (demo data).',
    stats: [['Completed Inspections', '41'], ['Pending Inspections', '6'], ['Temperature Deviations', '3'], ['Quality Alerts', '2']],
    table: inspector
  }
}
