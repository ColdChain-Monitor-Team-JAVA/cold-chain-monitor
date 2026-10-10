export const admin = {
  users: {
    title: 'User Overview', columns: ['Name', 'Role', 'Status', 'Last Login'],
    rows: [
      ['Rahul Sharma', 'Warehouse Manager', 'Active', '10 min ago'],
      ['Ananya Rao', 'Transport Manager', 'Active', '25 min ago'],
      ['Kiran Kumar', 'Driver', 'Active', '1 hour ago'],
      ['Priya Singh', 'Hospital Staff', 'Active', '2 hours ago']
    ]
  },
  overview: [['Active Shipments', '18'], ['Temperature Normal', '15'], ['Attention Required', '2'], ['Critical', '1']] as [string, string][]
}
export const warehouse = {
  title: 'Storage Units', columns: ['Storage Unit', 'Temperature', 'Humidity', 'Status'],
  rows: [
    ['Cold Room A', '4.1 °C', '52%', 'Normal'],
    ['Cold Room B', '3.7 °C', '49%', 'Normal'],
    ['Freezer 1', '-19.5 °C', '45%', 'Normal'],
    ['Cold Room C', '8.6 °C', '61%', 'Warning']
  ]
}
export const driver = {
  current: [
    ['Shipment ID', 'SHP-1001'], ['Pickup Location', 'Bengaluru Central Warehouse'],
    ['Destination', 'Chennai District Hospital'], ['Current Temperature', '4.2 °C'],
    ['Current Humidity', '50%'], ['Delivery Status', 'In Transit'], ['Route Status', 'On Schedule']
  ] as [string, string][],
  table: {
    title: 'Assigned Deliveries', columns: ['Shipment ID', 'Destination', 'Status'],
    rows: [['SHP-1001', 'Chennai', 'In Transit'], ['SHP-1006', 'Mysuru', 'Pending'], ['SHP-1007', 'Hosur', 'Pending'], ['SHP-1008', 'Salem', 'Pending']]
  }
}
export const hospital = {
  title: 'Incoming Shipments', columns: ['Shipment ID', 'Supplier', 'Expected Arrival', 'Temperature', 'Status'],
  rows: [
    ['SHP-2001', 'MediPharma Ltd', 'Today, 4:30 PM', '4.0 °C', 'In Transit'],
    ['SHP-2002', 'BioServe India', 'Tomorrow, 9:00 AM', '3.6 °C', 'Pending'],
    ['SHP-2003', 'VaxCare Supplies', 'Today, 1:15 PM', '8.4 °C', 'Warning']
  ]
}
export const supplier = {
  title: 'My Shipments', columns: ['Shipment ID', 'Destination', 'Dispatch Date', 'Expected Delivery', 'Status'],
  rows: [
    ['SHP-3001', 'City Hospital', '27 Sep 2026', '30 Sep 2026', 'In Transit'],
    ['SHP-3002', 'Apollo Clinic', '25 Sep 2026', '28 Sep 2026', 'Delivered'],
    ['SHP-3003', 'Regional Lab', '29 Sep 2026', '02 Oct 2026', 'Pending']
  ]
}
export const inspector = {
  title: 'Recent Inspections', columns: ['Shipment ID', 'Inspection Date', 'Temperature Status', 'Humidity Status', 'Inspection Result'],
  rows: [
    ['SHP-4001', '28 Sep 2026', 'Normal', 'Normal', 'Completed'],
    ['SHP-4002', '28 Sep 2026', 'Warning', 'Normal', 'Warning'],
    ['SHP-4003', '29 Sep 2026', 'Normal', 'Normal', 'Pending'],
    ['SHP-4004', '29 Sep 2026', 'Critical', 'Warning', 'Critical']
  ]
}
