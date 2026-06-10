import { useEffect, useState } from 'react'
import { Row, Col, Spin, Empty, Badge } from 'antd'
import { HeartFilled, EyeFilled } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { roomApi } from '../../api/business'
import type { LiveRoom } from '../../types'

export default function RoomsPage() {
  const [rooms, setRooms] = useState<LiveRoom[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  const load = () => {
    roomApi.list().then((res: any) => setRooms(res.data || [])).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  // 每 5 秒轮询在线人数
  useEffect(() => {
    const timer = setInterval(() => {
      roomApi.list().then((res: any) => {
        const data: LiveRoom[] = res.data || []
        setRooms(prev => prev.map(r => {
          const fresh = data.find(d => d.id === r.id)
          return fresh ? { ...r, online: fresh.online } : r
        }))
      }).catch(() => {})
    }, 5000)
    return () => clearInterval(timer)
  }, [])

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 'calc(100vh - 56px)' }}>
      <Spin size="large" />
    </div>
  )

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '20px 16px' }}>
      <div style={{ marginBottom: 24, display: 'flex', alignItems: 'baseline', gap: 12 }}>
        <h2 style={{ margin: 0, fontWeight: 800, fontSize: 22, color: '#fff' }}>热门直播</h2>
        <Badge count={`${rooms.length} 个直播间`}
          style={{ backgroundColor: 'rgba(254,44,85,0.2)', color: '#fe2c55', fontWeight: 600, fontSize: 12 }} />
      </div>

      {rooms.length === 0 ? (
        <Empty description={<span style={{ color: 'rgba(255,255,255,0.4)' }}>暂无直播中的直播间</span>}
          image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: 80 }} />
      ) : (
        <Row gutter={[16, 16]}>
          {rooms.map(r => (
            <Col key={r.id} xs={12} sm={8} md={6}>
              <div onClick={() => navigate(`/room/${r.id}`, { state: r })}
                style={{
                  cursor: 'pointer', borderRadius: 14, overflow: 'hidden',
                  background: '#1c1c1e', transition: 'transform 0.2s',
                  boxShadow: '0 4px 16px rgba(0,0,0,0.3)',
                }}
                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-4px)'}
                onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
              >
                <div style={{ position: 'relative', paddingBottom: '120%' }}>
                  {r.coverImage ? (
                    <img src={r.coverImage} alt={r.title}
                      style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <div style={{
                      position: 'absolute', inset: 0,
                      background: 'linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
                      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 8,
                    }}>
                      <div style={{ fontSize: 36 }}>📺</div>
                      <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: 12 }}>主播未设置封面</div>
                    </div>
                  )}
                  <div style={{
                    position: 'absolute', top: 8, left: 8,
                    background: 'linear-gradient(135deg, #fe2c55, #ff4d4f)',
                    color: '#fff', fontSize: 11, fontWeight: 700,
                    padding: '2px 8px', borderRadius: 4, display: 'flex', alignItems: 'center', gap: 3,
                  }}>
                    <span style={{ width: 6, height: 6, borderRadius: 3, background: '#fff', display: 'inline-block' }} /> 直播中
                  </div>
                  <div style={{
                    position: 'absolute', bottom: 8, left: 8, right: 8,
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  }}>
                    <div style={{
                      background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(8px)',
                      borderRadius: 20, padding: '3px 10px', color: '#fff', fontSize: 11,
                      display: 'flex', alignItems: 'center', gap: 4,
                    }}>
                      <EyeFilled /> {r.online || 0} 人
                    </div>
                    <div style={{
                      background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(8px)',
                      borderRadius: 20, padding: '3px 10px', color: '#fe2c55', fontSize: 11,
                      display: 'flex', alignItems: 'center', gap: 4,
                    }}>
                      <HeartFilled />
                    </div>
                  </div>
                </div>
                <div style={{ padding: '10px 12px' }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {r.title}
                  </div>
                  <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {r.notice || '主播正在激情介绍拍品…'}
                  </div>
                </div>
              </div>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}
