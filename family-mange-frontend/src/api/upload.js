import { http, unwrap } from './http'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  }).then(unwrap)
}
