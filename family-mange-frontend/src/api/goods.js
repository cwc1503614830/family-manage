import { http, unwrap } from './http'

export function listGoodsItems(params = {}) {
  return http.get('/goods/items', { params }).then(unwrap)
}

export function createGoodsItem(data) {
  return http.post('/goods/items', data).then(unwrap)
}

export function updateGoodsItem(id, data) {
  return http.put(`/goods/items/${id}`, data).then(unwrap)
}

export function deleteGoodsItem(id) {
  return http.delete(`/goods/items/${id}`).then(unwrap)
}

export function listGoodsLocations() {
  return http.get('/goods/locations').then(unwrap)
}

export function createGoodsLocation(data) {
  return http.post('/goods/locations', data).then(unwrap)
}

export function updateGoodsLocation(id, data) {
  return http.put(`/goods/locations/${id}`, data).then(unwrap)
}

export function deleteGoodsLocation(id) {
  return http.delete(`/goods/locations/${id}`).then(unwrap)
}

export function getGoodsOptions() {
  return http.get('/goods/options').then(unwrap)
}
