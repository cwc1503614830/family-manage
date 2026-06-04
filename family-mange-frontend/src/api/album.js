import { http, unwrap } from './http'

export function listAlbumThemes() {
  return http.get('/album/themes').then(unwrap)
}

export function createAlbumTheme(data) {
  return http.post('/album/themes', data).then(unwrap)
}

export function updateAlbumTheme(id, data) {
  return http.put(`/album/themes/${id}`, data).then(unwrap)
}

export function deleteAlbumTheme(id) {
  return http.delete(`/album/themes/${id}`).then(unwrap)
}

export function listAlbumCards() {
  return http.get('/album/cards').then(unwrap)
}

export function listAlbumPhotos(params = {}) {
  return http.get('/album/photos', { params }).then(unwrap)
}

export function createAlbumPhoto(data) {
  return http.post('/album/photos', data).then(unwrap)
}

export function createAlbumPhotosBatch(data) {
  return http.post('/album/photos/batch', data).then(unwrap)
}

export function updateAlbumPhoto(id, data) {
  return http.put(`/album/photos/${id}`, data).then(unwrap)
}

export function deleteAlbumPhoto(id) {
  return http.delete(`/album/photos/${id}`).then(unwrap)
}
