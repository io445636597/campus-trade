import request from './request'

// getList supports keyword param for search.
// The backend transparently uses Elasticsearch (with MySQL fallback) when a keyword is provided.
// The dedicated /api/product/search endpoint is also available via the search() function below.
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

export function getHot() {
  return request.get('/api/product/hot')
}

export function search(keyword, page, size) {
  return request.get('/api/product/search', { params: { keyword, page, size } })
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/product/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
