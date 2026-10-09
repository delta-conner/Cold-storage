import axios from 'axios'
import { Message } from 'element-ui'
import router from '../router'
import { getToken, clearAuth } from './auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

http.interceptors.response.use(
  res => {
    const data = res.data
    if (data && data.code !== 0) {
      Message.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    if (err.response && err.response.status === 401) {
      clearAuth()
      router.replace('/login')
      Message.error('登录已失效，请重新登录')
    } else {
      const msg = (err.response && err.response.data && err.response.data.message)
        || err.message
        || '网络错误'
      Message.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http
