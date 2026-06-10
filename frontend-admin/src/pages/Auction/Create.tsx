import { useEffect, useState } from 'react'
import { Form, Input, InputNumber, Select, Button, message, Card } from 'antd'
import { useNavigate } from 'react-router-dom'
import { auctionApi, roomApi } from '../../api/business'

export default function AuctionCreatePage() {
  const [loading, setLoading] = useState(false)
  const [roomStatus, setRoomStatus] = useState<number | null>(null)
  const [roomId, setRoomId] = useState<number | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    roomApi.getMyRoom().then((res: any) => {
      setRoomId(res.data.id)
      setRoomStatus(res.data.status)
    }).catch(() => {})
  }, [])

  const onFinish = async (values: any) => {
    if (!roomId) { message.error('请先创建直播间'); return }
    if (roomStatus !== 2) { message.error('请先开播'); return }
    setLoading(true)
    try {
      await auctionApi.create({ ...values, roomId })
      message.success('发布成功')
      navigate('/admin/auctions')
    } catch (e: any) {
      message.error(e.response?.data?.message || '发布失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="发布竞拍" style={{ maxWidth: 600 }}>
      <Form onFinish={onFinish} layout="vertical">
        <Form.Item name="name" label="商品名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label="商品描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="images" label="图片URL">
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="startPrice" label="起拍价" initialValue={0}>
          <InputNumber style={{ width: '100%' }} min={0} precision={2} />
        </Form.Item>
        <Form.Item name="incrementAmount" label="加价幅度" rules={[{ required: true }]}>
          <InputNumber style={{ width: '100%' }} min={0.01} precision={2} />
        </Form.Item>
        <Form.Item name="maxPrice" label="封顶价（留空不设上限）">
          <InputNumber style={{ width: '100%' }} min={0} precision={2} />
        </Form.Item>
        <Form.Item name="durationMinutes" label="竞拍时长(分钟)" rules={[{ required: true }]}>
          <Select options={[5, 10, 15, 30, 60].map(v => ({ value: v, label: `${v}分钟` }))} />
        </Form.Item>
        <Form.Item name="delaySeconds" label="延时秒数" initialValue={10}>
          <Select options={[10, 15, 20, 25, 30].map(v => ({ value: v, label: `${v}秒` }))} />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={loading} block>发布竞拍</Button>
      </Form>
    </Card>
  )
}
