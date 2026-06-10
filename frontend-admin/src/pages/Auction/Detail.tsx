import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Descriptions, Button, Tag, Card } from 'antd'
import { auctionApi } from '../../api/business'
import { AuctionStatusMap } from '../../types'
import type { AuctionItem } from '../../types'

export default function AuctionDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState<AuctionItem | null>(null)

  useEffect(() => {
    if (id) {
      auctionApi.detail(Number(id)).then((res: any) => setItem(res.data))
    }
  }, [id])

  if (!item) return null
  const status = AuctionStatusMap[item.status]

  return (
    <Card title={`${item.name} - 竞拍详情`}>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="状态"><Tag color={status.color}>{status.text}</Tag></Descriptions.Item>
        <Descriptions.Item label="商品名称">{item.name}</Descriptions.Item>
        <Descriptions.Item label="描述">{item.description || '无'}</Descriptions.Item>
        <Descriptions.Item label="图片">{item.images || '无'}</Descriptions.Item>
        <Descriptions.Item label="起拍价">¥{item.startPrice}</Descriptions.Item>
        <Descriptions.Item label="加价幅度">¥{item.incrementAmount}</Descriptions.Item>
        <Descriptions.Item label="封顶价">{item.maxPrice ? `¥${item.maxPrice}` : '无上限'}</Descriptions.Item>
        <Descriptions.Item label="时长">{item.durationMinutes}分钟</Descriptions.Item>
        <Descriptions.Item label="延时秒数">{item.delaySeconds}秒</Descriptions.Item>
        <Descriptions.Item label="当前最高价">¥{item.currentPrice}</Descriptions.Item>
        <Descriptions.Item label="最高出价者">{item.currentBidderName || '暂无'}</Descriptions.Item>
        <Descriptions.Item label="出价次数">{item.bidCount}</Descriptions.Item>
        <Descriptions.Item label="开始时间">{item.startTime || '-'}</Descriptions.Item>
        <Descriptions.Item label="计划结束时间">{item.plannedEndTime || '-'}</Descriptions.Item>
        <Descriptions.Item label="实际结束时间">{item.actualEndTime || '-'}</Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 16 }}>
        <Button onClick={() => navigate('/admin/auctions')}>返回列表</Button>
      </div>
    </Card>
  )
}
