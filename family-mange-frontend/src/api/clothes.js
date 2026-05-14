import { http, unwrap } from './http'

export function listClothesItems() {
  return http.get('/clothes/items').then(unwrap)
}

export function createClothesItem(data) {
  return http.post('/clothes/items', data).then(unwrap)
}

export function updateClothesItem(id, data) {
  return http.put(`/clothes/items/${id}`, data).then(unwrap)
}

export function deleteClothesItem(id) {
  return http.delete(`/clothes/items/${id}`).then(unwrap)
}

export function listClothesTypes() {
  return http.get('/clothes/types').then(unwrap)
}

export function createClothesType(data) {
  return http.post('/clothes/types', data).then(unwrap)
}

export function updateClothesType(id, data) {
  return http.put(`/clothes/types/${id}`, data).then(unwrap)
}

export function deleteClothesType(id) {
  return http.delete(`/clothes/types/${id}`).then(unwrap)
}

export function listClothesLocations() {
  return http.get('/clothes/locations').then(unwrap)
}

export function createClothesLocation(data) {
  return http.post('/clothes/locations', data).then(unwrap)
}

export function updateClothesLocation(id, data) {
  return http.put(`/clothes/locations/${id}`, data).then(unwrap)
}

export function deleteClothesLocation(id) {
  return http.delete(`/clothes/locations/${id}`).then(unwrap)
}

export function getClothesOptions() {
  return http.get('/clothes/options').then(unwrap)
}
