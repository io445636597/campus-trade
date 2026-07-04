import request from './request'

export function getList(params) {
  return request.get('/api/product', { params })
}

export function getDetail(id) {
  return request.get(`/api/product/${id}`)
}

export function create(data) {
  return request.post('/api/product', data)
}

export function update(id, data) {
  return request.put(`/api/product/${id}`, data)
}

export function updateStatus(id, status) {
  return request.patch(`/api/product/${id}/status`, { status })
}

export function remove(id) {
  return request.delete(`/api/product/${id}`)
}

export function search(keyword, page, size) {
  return request.get('/api/product/search', { params: { keyword, page, size } })
}
