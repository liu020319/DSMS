import request from '../utils/request'

export function getPrescriptionPage(params) {
  return request.get('/prescription/page', { params })
}

export function getPrescriptionByUserId(userId) {
  return request.get('/prescription/list/' + userId)
}

export function getPrescriptionDetail(id) {
  return request.get('/prescription/detail/' + id)
}

export function getPrescriptionHistory(id) {
  return request.get('/prescription/history/' + id)
}

export function addPrescription(data) {
  return request.post('/prescription/add', data)
}

export function updatePrescription(data, changeReason) {
  return request.put('/prescription/update', data, { params: { changeReason } })
}

export function stopPrescription(id) {
  return request.put('/prescription/stop/' + id)
}

export function enablePrescription(id) {
  return request.put('/prescription/enable/' + id)
}
