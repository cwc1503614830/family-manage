import { http, unwrap } from './http'

export function listWuhanSpots() {
  return http.get('/travel/wuhan/spots').then(unwrap)
}

export function createWuhanSpot(data) {
  return http.post('/travel/wuhan/spots', data).then(unwrap)
}

export function updateWuhanSpot(id, data) {
  return http.put(`/travel/wuhan/spots/${id}`, data).then(unwrap)
}

export function deleteWuhanSpot(id) {
  return http.delete(`/travel/wuhan/spots/${id}`).then(unwrap)
}

export function listDomesticTrips() {
  return http.get('/travel/domestic').then(unwrap)
}

export function createDomesticTrip(data) {
  return http.post('/travel/domestic', data).then(unwrap)
}

export function updateDomesticTrip(id, data) {
  return http.put(`/travel/domestic/${id}`, data).then(unwrap)
}

export function deleteDomesticTrip(id) {
  return http.delete(`/travel/domestic/${id}`).then(unwrap)
}

export function getDomesticNote(id) {
  return http.get(`/travel/domestic/${id}/note`).then(unwrap)
}

export function saveDomesticNote(id, data) {
  return http.put(`/travel/domestic/${id}/note`, data).then(unwrap)
}

export function getTravelOptions(province) {
  const params = province ? { province } : {}
  return http.get('/travel/options', { params }).then(unwrap)
}
