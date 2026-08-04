import request from '../utils/request'

export function submitApproval(data) {
  return request.post('/approval/submit', data)
}

export function approveTask(id, comment) {
  return request.put('/approval/approve/' + id, null, { params: { comment } })
}

export function rejectTask(id, comment) {
  return request.put('/approval/reject/' + id, null, { params: { comment } })
}

export function modifyAndApproveTask(id, data, comment) {
  return request.put('/approval/modify-approve/' + id, data, { params: { comment } })
}

export function getPendingList(params) {
  return request.get('/approval/pending', { params })
}

export function getMyTasks(params) {
  return request.get('/approval/my', { params })
}

export function getAllTasks(params) {
  return request.get('/approval/all', { params })
}

export function getTaskById(id) {
  return request.get('/approval/' + id)
}
