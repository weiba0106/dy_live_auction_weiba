import { useEffect, useState, useRef } from 'react'
import { useParams, useLocation } from 'react-router-dom'
import { Button, InputNumber, Tag, message, Spin, Empty, Modal } from 'antd'
import { FireOutlined, ClockCircleOutlined, ThunderboltOutlined, CrownOutlined } from '@ant-design/icons'
import { auctionApi } from '../../api/business'
import { useAuthStore } from '../../stores/auth'
import { AuctionStatusMap } from '../../types'
import type { AuctionItem, LiveRoom } from '../../types'

export default function RoomDetailPage() {
  const { roomId } = useParams()
  const location = useLocation()
  const room = location.state as LiveRoom | null
  const { isLoggedIn } = useAuthStore()

  const [items, setItems] = useState<AuctionItem[]>([])
  const [loading, setLoading] = useState(true)
  const [bidModal, setBidModal] = useState<{ open: boolean; item: AuctionItem | null; amount: number }>({ open: false, item: null, amount: 0 })
  const [submitting, setSubmitting] = useState(false)
  const [countdowns, setCountdowns] = useState<Record<number, string>>({})
  const [flash, setFlash] = useState<Record<number, boolean>>({})
  const [onlineCount, setOnlineCount] = useState(0)
  const [ranking, setRanking] = useState<Record<number, any[]>>({})
  const stompRef = useRef<any>(null)

  const loadItems = () => {
    if (!roomId) return
    setLoading(true)
    auctionApi.listByRoom(Number(roomId)).then((res: any) => {
      setItems(res.data || [])
    }).finally(() => setLoading(false))
  }
  useEffect(() => { loadItems(); loadRankings(); }, [roomId])

  const loadRankings = () => {
    items.filter(it => it.status === 2).forEach(it => {
      import('../../api/index').then(({ default: api }) => {
        api.get(`/auction/${it.id}/ranking`).then((res: any) => {
          setRanking(prev => ({ ...prev, [it.id]: res.data || [] }))
        }).catch(() => {})
      })
    })
  }

  // 初始加载在线人数
  useEffect(() => {
    if (!roomId) return
    import('../../api/index').then(({ default: api }) => {
      api.get(`/rooms/${roomId}/online`).then((res: any) => {
        setOnlineCount(res.data?.online || 0)
      }).catch(() => {})
    })
  }, [roomId])

  // 倒计时
  useEffect(() => {
    const timer = setInterval(() => {
      const cds: Record<number, string> = {}
      items.forEach(it => {
        if (it.status === 2 && it.actualEndTime) {
          const remain = Math.max(0, Math.floor((new Date(it.actualEndTime!).getTime() - Date.now()) / 1000))
          cds[it.id] = remain > 0 ? `${Math.floor(remain / 60)}:${String(remain % 60).padStart(2, '0')}` : '已结束'
        }
      })
      setCountdowns(cds)
    }, 200)
    return () => clearInterval(timer)
  }, [items])

  // 价格闪烁动画
  const triggerFlash = (itemId: number) => {
    setFlash(prev => ({ ...prev, [itemId]: true }))
    setTimeout(() => setFlash(prev => ({ ...prev, [itemId]: false })), 600)
  }

  // WebSocket
  useEffect(() => {
    if (!roomId || !isLoggedIn()) return
    const connect = () => {
      try {
        const sock = new (window as any).SockJS('/ws')
        const Stomp = (window as any).Stomp
        if (!Stomp) return
        const client = Stomp.over(sock)
        client.connect({}, () => {
          client.subscribe(`/topic/auction/${roomId}`, (msg: any) => {
            const data = JSON.parse(msg.body)
            if (data.type === 'BID') {
              triggerFlash(data.itemId)
              setItems(prev => prev.map(it =>
                it.id === data.itemId ? { ...it, currentPrice: data.price, currentBidderName: data.bidder, bidCount: data.count } : it
              ))
              // 刷新排行榜
              import('../../api/index').then(({ default: api }) => {
                api.get(`/auction/${data.itemId}/ranking`).then((res: any) => {
                  setRanking(prev => ({ ...prev, [data.itemId]: res.data || [] }))
                }).catch(() => {})
              })
            } else if (data.type === 'EVENT') {
              if (data.event === 'SOLD') message.success(`🏆 竞拍成交！`)
              else if (data.event === 'ENDED') message.info('竞拍已结束')
              else if (data.event === 'CANCELLED') message.warning('竞拍已取消')
              else if (data.event === 'DELAYED') message.info('🔥 有人出价，竞拍延时！')
              if (data.event === 'DELAYED' && data.newEndTime) {
                setItems(prev => prev.map(it => it.id === data.itemId ? { ...it, actualEndTime: data.newEndTime } : it))
              } else if (data.event !== 'DELAYED' && data.event !== 'STARTED') {
                loadItems()
              }
            } else if (data.type === 'ONLINE') {
              setOnlineCount(data.count)
            }
          })
        }, () => setTimeout(connect, 3000))
        stompRef.current = client
      } catch { }
    }
    connect()
    return () => { try { stompRef.current?.disconnect() } catch { } }
  }, [roomId])

  const openBidModal = (item: AuctionItem) => {
    if (!isLoggedIn()) { message.warning('请先登录'); return }
    setBidModal({ open: true, item, amount: Number((item.currentPrice + item.incrementAmount).toFixed(2)) })
  }

  const handleBid = async () => {
    if (!bidModal.item) return
    const min = getMinBid(bidModal.item)
    if (bidModal.amount < min) {
      message.warning(`出价不能低于 ¥${min}`)
      return
    }
    setSubmitting(true)
    try {
      await auctionApi.bid(bidModal.item.id, bidModal.amount)
      message.success('🔥 出价成功！')
      setBidModal({ open: false, item: null, amount: 0 })
    } catch (e: any) {
      console.error('出价失败', e)
      message.error(e.response?.data?.message || '出价失败')
    } finally {
      setSubmitting(false)
    }
  }

  const getMinBid = (item: AuctionItem) => Number((item.currentPrice + item.incrementAmount).toFixed(2))
  const activeItems = items.filter(it => it.status === 2)

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 'calc(100vh - 56px)' }}>
      <Spin size="large" />
    </div>
  )

  return (
    <div style={{ background: '#0f0f0f', minHeight: 'calc(100vh - 56px)', paddingBottom: 24 }}>
      {/* 视频区 */}
      <div style={{ width: '100%', maxWidth: 750, margin: '0 auto' }}>
        {room?.videoUrl ? (
          <video src={room.videoUrl} controls autoPlay loop muted playsInline crossOrigin="anonymous"
            onError={() => message.warning('视频加载失败，请检查视频地址')}
            style={{ width: '100%', display: 'block', background: '#000', borderRadius: '0 0 12px 12px' }} />
        ) : (
          <div style={{
            height: 240, background: 'linear-gradient(180deg, #1a1a2e 0%, #0f3460 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12,
            borderRadius: '0 0 12px 12px',
          }}>
            <span style={{ fontSize: 48, filter: 'grayscale(0.5)' }}>📺</span>
            <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: 14 }}>暂无直播画面</span>
          </div>
        )}
      </div>

      {/* 拍品列表 */}
      <div style={{ maxWidth: 750, margin: '0 auto', padding: '0 16px' }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          margin: '20px 0 12px',
        }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: '#fff' }}>{room?.title || '直播间'}</h3>
            <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', marginTop: 2 }}>
              {activeItems.length} 件竞拍中 · {items.length} 件商品 · 🔥 {onlineCount} 人正在围观
            </div>
          </div>
          <Tag color="red" style={{ background: 'rgba(254,44,85,0.15)', border: 'none', color: '#fe2c55', borderRadius: 20, fontWeight: 600 }}>
            {activeItems.length} 件竞拍中
          </Tag>
        </div>

        {items.length === 0 ? (
          <Empty description={<span style={{ color: 'rgba(255,255,255,0.3)' }}>暂无竞拍商品</span>}
            image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: 60 }} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {items.map(item => {
              const status = AuctionStatusMap[item.status]
              const isActive = item.status === 2
              const cd = countdowns[item.id]
              const urgent = cd && cd !== '已结束' && parseInt(cd.split(':')[0]) === 0 && parseInt(cd.split(':')[1]) <= 30
              let imageUrl = ''
              try { imageUrl = JSON.parse(item.images || '[]')[0] || '' } catch { }

              return (
                <div key={item.id} style={{
                  background: isActive ? '#1c1c1e' : 'rgba(28,28,30,0.5)',
                  borderRadius: 14, padding: 14,
                  border: isActive ? '1px solid rgba(255,255,255,0.08)' : '1px solid rgba(255,255,255,0.03)',
                  transition: 'all 0.15s',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  {/* 缩略图 */}
                  <div style={{
                    width: 72, height: 72, borderRadius: 10, flexShrink: 0,
                    background: imageUrl ? `url(${imageUrl}) center/cover` : 'linear-gradient(135deg, #2d2d30, #1c1c1e)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    border: isActive ? '1px solid rgba(254,44,85,0.2)' : '1px solid rgba(255,255,255,0.04)',
                  }}>
                    {!imageUrl && <span style={{ fontSize: 24, filter: 'grayscale(0.6)', opacity: .5 }}>📦</span>}
                  </div>

                  {/* 商品信息 */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                      <span style={{ fontSize: 15, fontWeight: 600, color: '#fff' }}>{item.name}</span>
                      <Tag color={status.color} style={{ margin: 0, fontSize: 11 }}>{status.text}</Tag>
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '3px 14px', fontSize: 12 }}>
                      <span style={{ color: 'rgba(255,255,255,0.5)' }}>
                        当前 <b style={{
                          color: flash[item.id] ? '#ff6b81' : '#fe2c55',
                          fontSize: 16, fontWeight: 700,
                          transition: 'color 0.3s',
                        }}>¥{item.currentPrice}</b>
                      </span>
                      <span style={{ color: 'rgba(255,255,255,0.35)' }}>加价 ¥{item.incrementAmount}</span>
                      <span style={{ color: 'rgba(255,255,255,0.35)' }}>{item.bidCount} 次</span>
                      {isActive && cd && (
                        <span style={{
                          color: urgent ? '#fe2c55' : cd === '已结束' ? 'rgba(255,255,255,0.3)' : '#ffa940',
                          fontWeight: urgent ? 700 : 400,
                        }}>
                          ⏱ {cd}
                        </span>
                      )}
                      {item.status === 4 && (
                        <span style={{ color: '#52c41a', fontWeight: 600 }}>¥{item.currentPrice} 成交</span>
                      )}
                    </div>
                  </div>

                  {/* 出价按钮 */}
                  {isActive && (
                    <Button type="primary" size="large"
                      icon={<ThunderboltOutlined />}
                      style={{
                        borderRadius: 24, height: 42, fontWeight: 700, flexShrink: 0,
                        background: 'linear-gradient(135deg, #fe2c55 0%, #ff4d4f 100%)',
                        border: 'none', boxShadow: '0 4px 12px rgba(254,44,85,0.4)',
                        padding: '0 20px',
                      }}
                      onClick={() => openBidModal(item)}>
                      出价
                    </Button>
                  )}
                  {item.status === 4 && item.currentBidderName && (
                    <Tag icon={<CrownOutlined />} color="gold" style={{ margin: 0, fontWeight: 600 }}>
                      {item.currentBidderName}
                    </Tag>
                  )}
                  </div>
                  {isActive && ranking[item.id]?.length > 0 && (
                    <div style={{
                      display: 'flex', gap: 10, paddingTop: 10, flexWrap: 'wrap',
                      borderTop: '1px solid rgba(255,255,255,0.04)',
                    }}>
                      {ranking[item.id].slice(0, 5).map((r: any) => (
                        <span key={r.rank} style={{
                          fontSize: 11, color: 'rgba(255,255,255,0.5)',
                          background: 'rgba(255,255,255,0.04)', borderRadius: 6,
                          padding: '2px 8px',
                        }}>
                          {r.rank === 1 ? '🥇' : r.rank === 2 ? '🥈' : r.rank === 3 ? '🥉' : `#${r.rank}`}
                          {' '}¥{r.amount}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* 出价弹窗 */}
      <Modal
        title={null}
        open={bidModal.open}
        onCancel={() => setBidModal({ open: false, item: null, amount: 0 })}
        footer={null}
        width={380}
        centered
        closable
        styles={{ content: { background: '#1c1c1e', borderRadius: 20, padding: '28px 24px', border: '1px solid rgba(255,255,255,0.06)' } }}
      >
        {bidModal.item && (
          <div>
            <div style={{ textAlign: 'center', marginBottom: 24 }}>
              <div style={{ fontSize: 14, color: 'rgba(255,255,255,0.5)', marginBottom: 4 }}>确认出价</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#fff' }}>{bidModal.item.name}</div>
            </div>

            <div style={{
              background: 'rgba(254,44,85,0.08)', borderRadius: 14, padding: '16px 20px',
              marginBottom: 20, textAlign: 'center', border: '1px solid rgba(254,44,85,0.12)',
            }}>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', marginBottom: 6 }}>当前最高价</div>
              <div style={{ fontSize: 32, fontWeight: 800, color: '#fe2c55', lineHeight: 1 }}>
                ¥{bidModal.item.currentPrice}
              </div>
            </div>

            <div style={{ marginBottom: 20 }}>
              <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', marginBottom: 10 }}>
                最低出价 <b style={{ color: '#ffa940' }}>¥{getMinBid(bidModal.item)}</b> · 加价 ¥{bidModal.item.incrementAmount}
              </div>
              <InputNumber
                style={{ width: '100%' }}
                size="large"
                value={bidModal.amount}
                onChange={v => v && setBidModal(prev => ({ ...prev, amount: v }))}
                min={getMinBid(bidModal.item)}
                step={bidModal.item.incrementAmount}
                precision={2}
                placeholder={`最低 ¥${getMinBid(bidModal.item)}`}
                prefix={<span style={{ color: '#fe2c55', fontWeight: 700, fontSize: 18, marginRight: 4 }}>¥</span>}
              />
            </div>

            {bidModal.item.maxPrice && (
              <div style={{
                fontSize: 12, color: 'rgba(255,255,255,0.35)', textAlign: 'center', marginBottom: 16,
              }}>
                封顶价 ¥{bidModal.item.maxPrice} · 达到封顶价自动成交
              </div>
            )}

            <Button type="primary" danger size="large" block loading={submitting}
              onClick={handleBid}
              style={{
                borderRadius: 14, height: 50, fontWeight: 700, fontSize: 16,
                background: 'linear-gradient(135deg, #fe2c55 0%, #ff4d4f 100%)',
                border: 'none', boxShadow: '0 4px 16px rgba(254,44,85,0.4)',
              }}>
              🔥 确认出价 ¥{bidModal.amount}
            </Button>
          </div>
        )}
      </Modal>
    </div>
  )
}
