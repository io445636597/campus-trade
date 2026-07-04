import request from './request'

export function getList(productId) {
  return request.get(`/api/product/${productId}/message`)
}

export function send(productId, content) {
  return request.post(`/api/product/${productId}/message`, { content })
}

export function remove(id) {
  return request.delete(`/api/message/${id}`)
}
