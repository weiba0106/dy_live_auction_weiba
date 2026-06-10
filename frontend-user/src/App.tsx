import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { ConfigProvider, theme, Layout, Button, Space, Avatar, Badge } from 'antd'
import {
  HomeOutlined, HistoryOutlined, OrderedListOutlined,
  LoginOutlined, LogoutOutlined, FireFilled,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from './stores/auth'
import LoginPage from './pages/Login'
import RoomsPage from './pages/Rooms'
import RoomDetailPage from './pages/RoomDetail'
import MyBidsPage from './pages/Bids'
import MyOrdersPage from './pages/Orders'

const { Header, Content } = Layout

function AppHeader() {
  const { nickname, isLoggedIn, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const isRoomDetail = location.pathname.startsWith('/room/')

  return (
    <Header style={{
      background: isRoomDetail ? 'transparent' : 'rgba(22,22,24,0.95)',
      backdropFilter: 'blur(20px)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 20px', height: 56,
      borderBottom: isRoomDetail ? 'none' : '1px solid rgba(255,255,255,0.06)',
      position: 'sticky', top: 0, zIndex: 100,
    }}>
      <Space size={20}>
        <span
          onClick={() => navigate('/')}
          style={{
            fontSize: 20, fontWeight: 800, cursor: 'pointer',
            background: 'linear-gradient(135deg, #fe2c55 0%, #ff6b81 100%)',
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            letterSpacing: 1,
          }}>
          直播竞拍
        </span>
        {!isRoomDetail && (
          <>
            <Button type="text" style={{ color: 'rgba(255,255,255,0.85)', fontWeight: 500 }}
              icon={<HomeOutlined />} onClick={() => navigate('/')}>首页</Button>
            {isLoggedIn() && <>
              <Button type="text" style={{ color: 'rgba(255,255,255,0.65)' }}
                icon={<HistoryOutlined />} onClick={() => navigate('/bids')}>出价</Button>
              <Button type="text" style={{ color: 'rgba(255,255,255,0.65)' }}
                icon={<OrderedListOutlined />} onClick={() => navigate('/orders')}>订单</Button>
            </>}
          </>
        )}
      </Space>
      <Space>
        {isLoggedIn() ? (
          <Space size={12}>
            <span style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13 }}>{nickname}</span>
            <Button size="small" ghost style={{ borderRadius: 20, borderColor: 'rgba(255,255,255,0.3)', color: 'rgba(255,255,255,0.7)' }}
              icon={<LogoutOutlined />} onClick={() => { logout(); navigate('/') }}>退出</Button>
          </Space>
        ) : (
          <Button type="primary" shape="round" icon={<LoginOutlined />}
            style={{ background: '#fe2c55', border: 'none', fontWeight: 600 }}
            onClick={() => navigate('/login')}>登录</Button>
        )}
      </Space>
    </Header>
  )
}

export default function App() {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#fe2c55',
          colorBgContainer: '#1c1c1e',
          colorBgLayout: '#0f0f0f',
          colorBorder: 'rgba(255,255,255,0.06)',
          borderRadius: 12,
          fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", sans-serif',
        },
      }}
    >
      <BrowserRouter>
        <Layout style={{ minHeight: '100vh', background: '#0f0f0f' }}>
          <AppHeader />
          <Content>
            <Routes>
              <Route path="/" element={<RoomsPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/room/:roomId" element={<RoomDetailPage />} />
              <Route path="/bids" element={<MyBidsPage />} />
              <Route path="/orders" element={<MyOrdersPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Content>
        </Layout>
      </BrowserRouter>
    </ConfigProvider>
  )
}
