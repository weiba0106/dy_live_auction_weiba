import { useEffect, useState } from 'react'
import { Table, Tag, Empty } from 'antd'
import { userApi } from '../../api/business'

export default function MyBidsPage() {
  const [list, setList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    userApi.myBids().then((res: any) => setList(res.data || [])).finally(() => setLoading(false))
  }, [])

  const columns = [
    { title: '拍品ID', dataIndex: 'itemId', width: 80 },
    { title: '出价金额', dataIndex: 'bidAmount', render: (v: number) => <b style={{ color: '#fe2c55' }}>¥{v}</b> },
    { title: '出价时间', dataIndex: 'bidTime' },
    {
      title: '状态', dataIndex: 'isValid',
      render: (v: number) => <Tag color={v ? 'green' : 'default'}>{v ? '领先' : '被超越'}</Tag>,
    },
  ]

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '20px 16px' }}>
      <h3 style={{ color: '#fff', fontWeight: 700, fontSize: 18, marginBottom: 16 }}>出价记录</h3>
      {list.length === 0 ? <Empty description={<span style={{ color: 'rgba(255,255,255,0.3)' }}>暂无出价记录</span>} image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: 60 }} /> :
        <Table dataSource={list} columns={columns} rowKey="id" loading={loading} size="small"
          style={{ background: 'transparent' }} pagination={false} />}
    </div>
  )
}
