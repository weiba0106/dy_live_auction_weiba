export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  role: number
}

export interface LoginResponse {
  token: string
  userId: number
  nickname: string
  role: number
}

export interface AuctionItem {
  id: number
  roomId: number
  name: string
  description: string
  images: string
  startPrice: number
  incrementAmount: number
  maxPrice: number | null
  durationMinutes: number
  delaySeconds: number
  currentPrice: number
  currentBidderName: string
  bidCount: number
  startTime: string
  plannedEndTime: string
  actualEndTime: string
  status: number
  statusDesc: string
}

export interface LiveRoom {
  id: number
  merchantId: number
  title: string
  coverImage: string
  status: number
  videoUrl: string
  notice: string
}

export interface Order {
  id: number
  itemId: number
  buyerId: number
  sellerId: number
  finalPrice: number
  status: number
  paidAt: string | null
  createdAt: string
}

export const AuctionStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '待开始', color: 'default' },
  2: { text: '竞拍中', color: 'processing' },
  3: { text: '已结束(流拍)', color: 'warning' },
  4: { text: '已成交', color: 'success' },
  5: { text: '已取消', color: 'error' },
}

export const RoomStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '未开播', color: 'default' },
  2: { text: '直播中', color: 'processing' },
  3: { text: '已结束', color: 'default' },
}
