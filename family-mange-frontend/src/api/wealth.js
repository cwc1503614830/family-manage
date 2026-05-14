import { http, unwrap } from './http'

export function listWealthAssets() {
  return http.get('/wealth/assets').then(unwrap)
}

export function createWealthAsset(data) {
  return http.post('/wealth/assets', data).then(unwrap)
}

export function updateWealthAsset(id, data) {
  return http.put(`/wealth/assets/${id}`, data).then(unwrap)
}

export function deleteWealthAsset(id) {
  return http.delete(`/wealth/assets/${id}`).then(unwrap)
}

export function listWealthTypes() {
  return http.get('/wealth/types').then(unwrap)
}

export function createWealthType(data) {
  return http.post('/wealth/types', data).then(unwrap)
}

export function updateWealthType(id, data) {
  return http.put(`/wealth/types/${id}`, data).then(unwrap)
}

export function deleteWealthType(id) {
  return http.delete(`/wealth/types/${id}`).then(unwrap)
}

export function getWealthOptions() {
  return http.get('/wealth/options').then(unwrap)
}
