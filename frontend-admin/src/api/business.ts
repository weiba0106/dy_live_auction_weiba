import api from './index'

export const roomApi = {
  getMyRoom: () => api.get('/admin/room'),
  create: (params: Record<string, string>) =>
    api.post('/admin/room', null, { params }),
  start: (id: number) => api.post(`/admin/room/${id}/start`),
  stop: (id: number) => api.post(`/admin/room/${id}/stop`),
}

export const auctionApi = {
  create: (data: Record<string, any>) => api.post('/admin/auction', data),
  update: (id: number, data: Record<string, any>) => api.put(`/admin/auction/${id}`, data),
  myList: (page: number, size: number) => api.get('/admin/auctions', { params: { page, size } }),
  detail: (id: number) => api.get(`/admin/auction/${id}`),
  start: (id: number) => api.post(`/admin/auction/${id}/start`),
  cancel: (id: number, reason: string) => api.post(`/admin/auction/${id}/cancel`, null, { params: { reason } }),
}

export const orderApi = {
  myOrders: () => api.get('/admin/orders'),
}
