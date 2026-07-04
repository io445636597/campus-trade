import request from './request'

export function register(data) {
  return request.post('/api/user/register', data)
}

export function login(data) {
  return request.post('/api/user/login', data)
}

export function getMe() {
  return request.get('/api/user/me')
}

export function updateMe(data) {
  return request.put('/api/user/me', data)
}

export function getUserById(id) {
  return request.get(`/api/user/${id}`)
}

export function getUserProducts(id) {
  return request.get(`/api/user/${id}/products`)
}
