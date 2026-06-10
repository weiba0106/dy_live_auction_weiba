import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Form, Input, InputNumber, Select, Button, message, Card } from 'antd'
import { auctionApi } from '../../api/business'
import type { AuctionItem } from '../../types'

export default function AuctionEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [item, setItem] = useState<AuctionItem | null>(null)

  useEffect(() => {
    if (id) {
      auctionApi.detail(Number(id)).then((res: any) => {
        const d = res.data
        setItem(d)
        if (d.status !== 1) {
          message.warning('只有待开始的竞拍可以修改规则')
          navigate('/admin/auctions')
          return
        }
        form.setFieldsValue(d)
      })
    }
  }, [id])

  const onFinish = async (values: any) => {
    if (!id) return
    setLoading(true)
    try {
      await auctionApi.update(Number(id), values)
      message.success('修改成功')
      navigate('/admin/auctions')
    } catch (e: any) {
      message.error(e.response?.data?.message || '修改失败')
    } finally {
      setLoading(false)
    }
  }

  if (!item) return null

  return (
    <Card title="修改竞拍规则" style={{ maxWidth: 600 }}>
      <Form form={form} onFinish={onFinish} layout="vertical">
        <Form.Item name="name" label="商品名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label="商品描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="images" label="图片URL">
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="startPrice" label="起拍价">
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
        <Form.Item name="delaySeconds" label="延时秒数">
          <Select options={[10, 15, 20, 25, 30].map(v => ({ value: v, label: `${v}秒` }))} />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={loading} block>保存修改</Button>
      </Form>
    </Card>
  )
}
