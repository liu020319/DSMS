import request from '../utils/request'

export function getAdminDashboard() {
  return request.get('/dashboard/admin')
}

export function getElderDashboard(userId) {
  return request.get('/dashboard/elder', { params: { userId } })
}

export function getAllStock(userId) {
  return request.get('/dashboard/stock/all', { params: { userId } })
}

export function getWarningStock(userId) {
  return request.get('/dashboard/stock/warning', { params: { userId } })
}

export function getExpiringStock(userId) {
  return request.get('/dashboard/stock/expiring', { params: { userId } })
}

export function calcBoxes(prescriptionId, days) {
  return request.get('/dashboard/stock/calc-boxes', { params: { prescriptionId, days } })
}

export function manualAdjustStock(stockId, adjustUnits, reason) {
  return request.post('/dashboard/stock/manual-adjust', null, { params: { stockId, adjustUnits, reason } })
}
