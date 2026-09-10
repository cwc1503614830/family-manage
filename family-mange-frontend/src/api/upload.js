import { http, unwrap } from './http'
import { compressImage } from '../utils/compressImage'

export async function uploadFile(file, folder = 'clothes') {
  const compressed = await compressImage(file)
  const formData = new FormData()
  formData.append('file', compressed)
  return http.post('/files/upload', formData, {
    params: { folder },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  }).then(unwrap)
}
