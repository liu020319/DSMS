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

export function getMonthlyStats(params) {
  return request.get('/purchase/stats/monthly', { params })
}

export function getDailyStats(params) {
  return request.get('/purchase/stats/daily', { params })
}

export function getYearlyStats(params) {
  return request.get('/purchase/stats/yearly', { params })
}

export const getWeeklyStats = params => request.get('/purchase/stats/weekly', { params })
export const getPlatformStats = params => request.get('/purchase/stats/platform', { params })
export const getChannelStats = params => request.get('/purchase/stats/channel', { params })
export const getTimeBucketStats = params => request.get('/purchase/stats/time-bucket', { params })
export const getExpenseSummary = params => request.get('/purchase/stats/summary', { params })
