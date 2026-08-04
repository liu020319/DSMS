import request from '../utils/request'

export function getPurchasePage(params) {
  return request.get('/purchase/page', { params })
}

export function addPurchase(data) {
  return request.post('/purchase/add', data)
}

export function updatePurchase(data) {
  return request.put('/purchase/update', data)
}

export function deletePurchase(id) {
  return request.delete('/purchase/delete/' + id)
}

export function confirmReceipt(id) {
  return request.put('/purchase/confirm-receipt/' + id)
}

export function getMonthlyStats(userId) {
  return request.get('/purchase/stats/monthly', { params: { userId } })
}

export function getDailyStats(userId, days) {
  return request.get('/purchase/stats/daily', { params: { userId, days } })
}

export function getYearlyStats(userId) {
  return request.get('/purchase/stats/yearly', { params: { userId } })
}
