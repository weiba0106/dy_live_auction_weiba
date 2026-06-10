import api from './index'

export const roomApi = {
  list: () => api.get('/rooms'),
}

export const auctionApi = {
  listByRoom: (roomId: number) => api.get(`/room/${roomId}/auctions`),
  detail: (id: number) => api.get(`/auction/${id}`),
  bid: (id: number, amount: number) => api.post(`/auction/${id}/bid`, null, { params: { amount } }),
}

export const userApi = {
  myBids: () => api.get('/user/bids'),
  myOrders: () => api.get('/user/orders'),
  pay: (orderId: number) => api.post(`/order/${orderId}/pay`),
}
