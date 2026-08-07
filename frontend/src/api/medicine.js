import request from '../utils/request'

export function getMedicinePage(params) {
  return request.get('/medicine/page', { params })
}

export function getMedicineList() {
  return request.get('/medicine/list')
}

export function getMedicineById(id) {
  return request.get('/medicine/' + id)
}

export function addMedicine(data) {
  return request.post('/medicine/add', data)
}

export function updateMedicine(data) {
  return request.put('/medicine/update', data)
}

export function disableMedicine(id) {
  return request.put('/medicine/disable/' + id)
}

export function deleteMedicine(id) {
  return request.delete('/medicine/delete/' + id)
}
