import { useEffect, useState } from 'react'
import { Table, Button, Tag, message, Modal } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { auctionApi } from '../../api/business'
import { AuctionStatusMap } from '../../types'
import type { AuctionItem } from '../../types'

export default function AuctionListPage() {
  const [list, setList] = useState<AuctionItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const load = async (p: number) => {
    setLoading(true)
    try {
      const res: any = await auctionApi.myList(p, 10)
      setList(res.data?.records || [])
      setTotal(res.data?.total || 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(page) }, [page])

  const handleStart = async (id: number) => {
    await auctionApi.start(id)
    message.success('竞拍已开始')
    load(page)
  }

  const handleCancel = (id: number) => {
    Modal.confirm({
      title: '取消竞拍',
      content: <Input id="cancel-reason" placeholder="请输入取消原因" />,
      onOk: async () => {
        const reason = (document.getElementById('cancel-reason') as HTMLInputElement)?.value || '主播取消'
        await auctionApi.cancel(id, reason)
        message.success('竞拍已取消')
        load(page)
      },
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '商品名', dataIndex: 'name' },
    { title: '当前价', dataIndex: 'currentPrice', render: (v: number) => `¥${v}` },
    { title: '出价数', dataIndex: 'bidCount' },
    {
      title: '状态', dataIndex: 'status',
      render: (v: number) => {
        const s = AuctionStatusMap[v]
        return <Tag color={s?.color}>{s?.text}</Tag>
      },
    },
    {
      title: '操作', key: 'actions',
      render: (_: any, record: AuctionItem) => (
        <>
          {record.status === 1 && (
            <>
              <Button type="link" size="small" onClick={() => handleStart(record.id)}>开始</Button>
              <Button type="link" size="small" onClick={() => navigate(`/admin/auctions/${record.id}/edit`)}>修改</Button>
            </>
          )}
          {record.status === 2 && (
            <Button type="link" size="small" danger onClick={() => handleCancel(record.id)}>取消</Button>
          )}
          <Button type="link" size="small" onClick={() => navigate(`/admin/auctions/${record.id}`)}>详情</Button>
        </>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h3>竞拍管理</h3>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/admin/auctions/create')}>发布竞拍</Button>
      </div>
      <Table
        dataSource={list}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ current: page, total, onChange: setPage }}
      />
    </div>
  )
}
