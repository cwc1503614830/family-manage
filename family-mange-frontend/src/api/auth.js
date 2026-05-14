import { http, setAuthToken, unwrap } from './http'

export { setAuthToken }

export function login(data) {
  return http.post('/auth/login', data).then(unwrap)
}

export function currentUser() {
  return http.get('/auth/me').then(unwrap)
}

export function logout() {
  return http.post('/auth/logout').then(unwrap)
}
