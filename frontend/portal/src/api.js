import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token && !config.skipAuth) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(response => {
  const result = response.data
  if (result.code !== 200) {
    const error = new Error(result.message || '请求失败')
    error.code = result.code
    error.data = result.data
    return Promise.reject(error)
  }
  return result
}, error => {
  const serverResult = error.response?.data
  if (serverResult?.message) error.message = serverResult.message
  if (error.response?.status === 401 && !error.config?.skipAuth) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    location.hash = '#/login'
  }
  return Promise.reject(error)
})

export const authApi = {
  challenge: () => api.get('/auth/human-challenge', { skipAuth: true }),
  verify: challengeId => api.post('/auth/human-challenge/verify', { challengeId }, { skipAuth: true }),
  login: data => api.post('/auth/login', data, { skipAuth: true }),
  portalRegister: data => api.post('/auth/portal-register', data, { skipAuth: true })
}

export const financeApi = {
  ledgers: () => api.get('/portal/finance/ledgers'),
  createLedger: data => api.post('/portal/finance/ledgers', data),
  accounts: ledgerId => api.get('/portal/finance/accounts', { params: { ledgerId } }),
  createAccount: data => api.post('/portal/finance/accounts', data),
  transactions: (ledgerId, month) => api.get('/portal/finance/transactions', { params: { ledgerId, month } }),
  createTransaction: data => api.post('/portal/finance/transactions', data),
  deleteTransaction: transactionId => api.delete('/portal/finance/transactions', { params: { transactionId } }),
  budgets: (ledgerId, month) => api.get('/portal/finance/budgets', { params: { ledgerId, month } }),
  saveBudget: data => api.post('/portal/finance/budgets', data),
  summary: (ledgerId, month) => api.get('/portal/finance/summary', { params: { ledgerId, month } }),
  suggestions: ledgerId => api.get('/portal/finance/suggestions', { params: { ledgerId } })
}

export const serviceApi = {
  requests: status => api.get('/portal/services/requests', { params: status ? { status } : {} }),
  createRequest: data => api.post('/portal/services/requests', data),
  detail: id => api.get(`/portal/services/requests/${id}`),
  updateStatus: (id, data) => api.put(`/portal/services/requests/${id}/status`, data),
  cancel: id => api.put(`/portal/services/requests/${id}/cancel`),
  createMilestone: (id, data) => api.post(`/portal/services/requests/${id}/milestones`, data),
  workOrders: status => api.get('/portal/services/work-orders', { params: status ? { status } : {} }),
  createWorkOrder: data => api.post('/portal/services/work-orders', data),
  updateWorkOrder: (id, data) => api.put(`/portal/services/work-orders/${id}/status`, data)
}

export const publicServiceApi = {
  submitInquiry: data => api.post('/public/services/inquiries', data, { skipAuth: true }),
  queryInquiry: data => api.post('/public/services/inquiries/query', data, { skipAuth: true }),
  addMessage: data => api.post('/public/services/inquiries/messages', data, { skipAuth: true })
}

export const publicInquiryAdminApi = {
  list: status => api.get('/portal/services/public-inquiries', { params: status ? { status } : {} }),
  detail: id => api.get(`/portal/services/public-inquiries/${id}`),
  updateStatus: (id, status) => api.put(`/portal/services/public-inquiries/${id}/status`, { status }),
  reply: (id, messageText) => api.post(`/portal/services/public-inquiries/${id}/replies`, { messageText })
}

export function reportError(error, fallback = '操作失败') {
  ElMessage.error(error?.message || fallback)
}
