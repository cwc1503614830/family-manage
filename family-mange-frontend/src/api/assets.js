import { http, unwrap } from './http'

export function listAssets(params) {
  return http.get('/assets', { params }).then(unwrap)
}

export function getSummary() {
  return http.get('/assets/summary').then(unwrap)
}

export function getOptions() {
  return http.get('/assets/options').then(unwrap)
}

export function createAsset(data) {
  return http.post('/assets', data).then(unwrap)
}

export function updateAsset(id, data) {
  return http.put(`/assets/${id}`, data).then(unwrap)
}

export function deleteAsset(id) {
  return http.delete(`/assets/${id}`).then(unwrap)
}

export function changeStock(id, delta) {
  return http.patch(`/assets/${id}/stock`, { delta }).then(unwrap)
}
