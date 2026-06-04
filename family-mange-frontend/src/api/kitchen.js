import { http, unwrap } from './http'

export function listKitchenDishes(params = {}) {
  return http.get('/kitchen/dishes', { params }).then(unwrap)
}

export function createKitchenDish(data) {
  return http.post('/kitchen/dishes', data).then(unwrap)
}

export function updateKitchenDish(id, data) {
  return http.put(`/kitchen/dishes/${id}`, data).then(unwrap)
}

export function deleteKitchenDish(id) {
  return http.delete(`/kitchen/dishes/${id}`).then(unwrap)
}

export function listKitchenIngredients(params = {}) {
  return http.get('/kitchen/ingredients', { params }).then(unwrap)
}

export function createKitchenIngredient(data) {
  return http.post('/kitchen/ingredients', data).then(unwrap)
}

export function updateKitchenIngredient(id, data) {
  return http.put(`/kitchen/ingredients/${id}`, data).then(unwrap)
}

export function deleteKitchenIngredient(id) {
  return http.delete(`/kitchen/ingredients/${id}`).then(unwrap)
}

export function listKitchenCookRecords(params = {}) {
  return http.get('/kitchen/cook-records', { params }).then(unwrap)
}

export function createKitchenCookRecord(data) {
  return http.post('/kitchen/cook-records', data).then(unwrap)
}

export function orderKitchenFromCart(data) {
  return http.post('/kitchen/cook-records/order', data).then(unwrap)
}

export function updateKitchenCookRecord(id, data) {
  return http.put(`/kitchen/cook-records/${id}`, data).then(unwrap)
}

export function completeKitchenCookRecord(id) {
  return http.put(`/kitchen/cook-records/${id}/complete`).then(unwrap)
}

export function deleteKitchenCookRecord(id) {
  return http.delete(`/kitchen/cook-records/${id}`).then(unwrap)
}
