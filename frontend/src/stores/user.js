import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const role = ref(userInfo.value.role || '')

  function setLogin(data) {
    token.value = data.token
    userInfo.value = data
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function isAdmin() {
    return role.value === 'ADMIN'
  }

  function isElder() {
    return role.value === 'ELDER'
  }

  return { token, userInfo, role, setLogin, logout, isAdmin, isElder }
})
