export interface LiveRoom {
  id: number
  title: string
  coverImage: string
  videoUrl: string
  status: number
  notice: string
  online?: number
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
}

export const AuctionStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '即将开始', color: 'default' },
  2: { text: '竞拍中', color: 'processing' },
  3: { text: '已结束', color: 'warning' },
  4: { text: '已成交', color: 'success' },
  5: { text: '已取消', color: 'error' },
}
