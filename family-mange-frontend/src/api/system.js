import { http, unwrap } from './http'

export function listUsers() {
  return http.get('/system/users').then(unwrap)
}

export function createUser(data) {
  return http.post('/system/users', data).then(unwrap)
}

export function updateUser(id, data) {
  return http.put(`/system/users/${id}`, data).then(unwrap)
}

export function deleteUser(id) {
  return http.delete(`/system/users/${id}`).then(unwrap)
}

export function listRoles() {
  return http.get('/system/roles').then(unwrap)
}

export function createRole(data) {
  return http.post('/system/roles', data).then(unwrap)
}

export function updateRole(id, data) {
  return http.put(`/system/roles/${id}`, data).then(unwrap)
}

export function deleteRole(id) {
  return http.delete(`/system/roles/${id}`).then(unwrap)
}

export function listMenus() {
  return http.get('/system/menus').then(unwrap)
}

export function createMenu(data) {
  return http.post('/system/menus', data).then(unwrap)
}

export function updateMenu(id, data) {
  return http.put(`/system/menus/${id}`, data).then(unwrap)
}

export function deleteMenu(id) {
  return http.delete(`/system/menus/${id}`).then(unwrap)
}
