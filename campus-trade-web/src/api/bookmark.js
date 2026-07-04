import request from './request'

export function toggle(productId) {
  return request.post(`/api/product/${productId}/bookmark`)
}

export function getMyBookmarks() {
  return request.get('/api/user/bookmarks')
}
