import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000
})

export function setAuthToken(token) {
  if (token) {
    http.defaults.headers.common.Authorization = `Bearer ${token}`
  } else {
    delete http.defaults.headers.common.Authorization
  }
}

export function unwrap(response) {
  const body = response.data
  if (body && body.code !== 200) {
    const error = new Error(body.message || '接口请求失败')
    error.code = body.code
    throw error
  }
  return body ? body.data : null
}
