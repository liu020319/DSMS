import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.skipErrorMessage) ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      const appError = new Error(res.message || '请求失败')
      appError.code = res.code
      appError.data = res.data
      return Promise.reject(appError)
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
    const status = error.response?.status
    const message = status === 413
      ? '图片过大，请选择不超过12MB的图片'
      : (error.response?.data?.message || error.message || '网络错误')
    if (!error.config?.skipErrorMessage) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
