import request from '../utils/request'

export function login(data) {
  return request.post('/auth/login', data, { skipErrorMessage: true })
}

export function createHumanChallenge() {
  return request.get('/auth/human-challenge', { skipErrorMessage: true })
}

export function verifyHumanChallenge(challengeId) {
  return request.post('/auth/human-challenge/verify', { challengeId }, { skipErrorMessage: true })
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getUserList(role) {
  return request.get('/user/list', { params: { role } })
}

export function getUserById(id) {
  return request.get('/user/' + id)
}

export function addUser(data) {
  return request.post('/user/add', data)
}

export function updateUser(data) {
  return request.put('/user/update', data)
}

export function resetPassword(id, newPassword) {
  return request.put('/user/reset-password/' + id, { newPassword })
}

export function unlockUser(id) {
  return request.put('/user/unlock/' + id)
}

export function changePassword(data) {
  return request.put('/user/change-password', data)
}

export function bindElder(elderId, parentId) {
  return request.put('/user/bind', null, { params: { elderId, parentId } })
}

export function unbindElder(elderId) {
  return request.put('/user/unbind', null, { params: { elderId } })
}

export function getEldersByParent(parentId) {
  return request.get('/user/elders/' + parentId)
}
