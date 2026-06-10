import { useEffect, useState } from 'react'
import { Table, Button, Tag, message, Empty } from 'antd'
import { DollarOutlined } from '@ant-design/icons'
import { userApi } from '../../api/business'

const OrderStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '待付款', color: 'orange' },
  2: { text: '已付款', color: 'green' },
  3: { text: '已取消', color: 'default' },
}

export default function MyOrdersPage() {
  const [list, setList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const load = () => {
    setLoading(true)
    userApi.myOrders().then((res: any) => setList(res.data || [])).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const handlePay = async (orderId: number) => {
    try { await userApi.pay(orderId); message.success('🎉 支付成功！'); load() }
    catch (e: any) { message.error(e.response?.data?.message || '支付失败') }
  }

  const columns = [
    { title: '订单', dataIndex: 'id', width: 70, render: (v: number) => <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: 12 }}>#{v}</span> },
    { title: '拍品ID', dataIndex: 'itemId', width: 70 },
    { title: '成交价', dataIndex: 'finalPrice', render: (v: number) => <b style={{ color: '#fe2c55', fontSize: 15 }}>¥{v}</b> },
    { title: '状态', dataIndex: 'status', render: (v: number) => <Tag color={OrderStatusMap[v]?.color}>{OrderStatusMap[v]?.text}</Tag> },
    { title: '成交时间', dataIndex: 'createdAt' },
    { title: '付款时间', dataIndex: 'paidAt', render: (v: string | null) => v || '-' },
    {
      title: '', key: 'actions', width: 80,
      render: (_: any, r: any) =>
        r.status === 1 ? <Button type="primary" size="small" ghost icon={<DollarOutlined />} style={{ borderRadius: 16, borderColor: '#fe2c55', color: '#fe2c55' }} onClick={() => handlePay(r.id)}>付款</Button> : null,
    },
  ]

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '20px 16px' }}>
      <h3 style={{ color: '#fff', fontWeight: 700, fontSize: 18, marginBottom: 16 }}>我的订单</h3>
      {list.length === 0 ? <Empty description={<span style={{ color: 'rgba(255,255,255,0.3)' }}>暂无订单</span>} image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: 60 }} /> :
        <Table dataSource={list} columns={columns} rowKey="id" loading={loading} size="small"
          style={{ background: 'transparent' }}
          pagination={false} />}
    </div>
  )
}
