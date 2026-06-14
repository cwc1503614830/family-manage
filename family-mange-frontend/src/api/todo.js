import { http, unwrap } from './http'

export function listTodoItems(params = {}) {
  return http.get('/todo/items', { params }).then(unwrap)
}

export function createTodoItem(data) {
  return http.post('/todo/items', data).then(unwrap)
}

export function updateTodoItem(id, data) {
  return http.put(`/todo/items/${id}`, data).then(unwrap)
}

export function deleteTodoItem(id) {
  return http.delete(`/todo/items/${id}`).then(unwrap)
}
