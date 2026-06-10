import { useEffect, useState } from 'react'
import { Form, Input, Button, Card, Tag, message, Descriptions } from 'antd'
import { roomApi } from '../../api/business'
import type { LiveRoom } from '../../types'
import { RoomStatusMap } from '../../types'

export default function RoomPage() {
  const [room, setRoom] = useState<LiveRoom | null>(null)
  const [loading, setLoading] = useState(false)

  const loadRoom = async () => {
    try {
      const res: any = await roomApi.getMyRoom()
      if (res.data) setRoom(res.data)
    } catch { /* no room yet */ }
  }

  useEffect(() => { loadRoom() }, [])

  const handleCreate = async (values: any) => {
    setLoading(true)
    try {
      const res: any = await roomApi.create(values)
      setRoom(res.data)
      message.success('直播间已创建')
    } catch (e: any) {
      message.error(e.response?.data?.message || '创建失败')
    } finally {
      setLoading(false)
    }
  }

  const handleStart = async () => {
    if (!room) return
    await roomApi.start(room.id)
    message.success('开播成功')
    loadRoom()
  }

  const handleStop = async () => {
    if (!room) return
    await roomApi.stop(room.id)
    message.success('已下播')
    loadRoom()
  }

  if (!room) {
    return (
      <Card title="创建直播间" style={{ maxWidth: 500 }}>
        <div style={{ color: '#999', marginBottom: 12 }}>首次使用请先创建直播间</div>
        <Form onFinish={handleCreate} layout="vertical">
          <Form.Item name="title" label="直播间标题" rules={[{ required: true }]}>
            <Input placeholder="如：老王翡翠专场" />
          </Form.Item>
          <Form.Item name="coverImage" label="封面图URL">
            <Input placeholder="https://..." />
          </Form.Item>
          <Form.Item name="videoUrl" label="直播视频URL" help="支持 .mp4 等直链，若无法播放请检查地址或换一个 CDN 地址">
            <Input placeholder="如 https://example.com/live.mp4" />
          </Form.Item>
          <Form.Item name="notice" label="公告">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} block>创建直播间</Button>
        </Form>
      </Card>
    )
  }

  const status = RoomStatusMap[room.status]
  return (
    <div>
      <Descriptions title="我的直播间" column={2} bordered>
        <Descriptions.Item label="标题">{room.title}</Descriptions.Item>
        <Descriptions.Item label="状态"><Tag color={status.color}>{status.text}</Tag></Descriptions.Item>
        <Descriptions.Item label="封面">{room.coverImage || '未设置'}</Descriptions.Item>
        <Descriptions.Item label="视频地址">{room.videoUrl || '未设置'}</Descriptions.Item>
        <Descriptions.Item label="公告" span={2}>{room.notice || '无'}</Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 16, display: 'flex', gap: 12 }}>
        {(room.status === 1 || room.status === 3) && (
          <Button type="primary" onClick={handleStart}>开播</Button>
        )}
        {room.status === 2 && (
          <Button danger onClick={handleStop}>下播</Button>
        )}
      </div>
      {room.videoUrl && (
        <div style={{ marginTop: 16 }}>
          <h4>视频预览</h4>
          <video src={room.videoUrl} controls loop muted playsInline crossOrigin="anonymous" style={{ width: '100%', maxWidth: 480, borderRadius: 8, background: '#000' }} />
        </div>
      )}
    </div>
  )
}
