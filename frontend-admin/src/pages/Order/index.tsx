import { useEffect, useState } from 'react'
import { Table, Tag } from 'antd'
import { orderApi } from '../../api/business'
import type { Order } from '../../types'

const OrderStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '待付款', color: 'orange' },
  2: { text: '已付款', color: 'green' },
  3: { text: '已取消', color: 'default' },
}

export default function OrderListPage() {
  const [list, setList] = useState<Order[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    orderApi.myOrders().then((res: any) => setList(res.data || [])).finally(() => setLoading(false))
  }, [])

  const columns = [
    { title: '订单ID', dataIndex: 'id', width: 80 },
    { title: '拍品ID', dataIndex: 'itemId', width: 80 },
    { title: '成交价', dataIndex: 'finalPrice', render: (v: number) => `¥${v}` },
    { title: '买家ID', dataIndex: 'buyerId' },
    {
      title: '状态', dataIndex: 'status',
      render: (v: number) => {
        const s = OrderStatusMap[v]
        return <Tag color={s?.color}>{s?.text}</Tag>
      },
    },
    { title: '成交时间', dataIndex: 'createdAt' },
    { title: '付款时间', dataIndex: 'paidAt', render: (v: string | null) => v || '-' },
  ]

  return (
    <div>
      <h3>订单管理</h3>
      <Table dataSource={list} columns={columns} rowKey="id" loading={loading} />
    </div>
  )
}
