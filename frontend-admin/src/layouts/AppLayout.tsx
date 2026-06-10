import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Space } from 'antd'
import { ShopOutlined, OrderedListOutlined, PlayCircleOutlined, LogoutOutlined } from '@ant-design/icons'
import { useAuthStore } from '../stores/auth'

const { Header, Sider, Content } = Layout

const menuItems = [
  { key: '/admin/room', icon: <PlayCircleOutlined />, label: '直播间' },
  { key: '/admin/auctions', icon: <ShopOutlined />, label: '竞拍管理' },
  { key: '/admin/orders', icon: <OrderedListOutlined />, label: '订单管理' },
]

export default function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { nickname, logout } = useAuthStore()

  return (
    <Layout style={{ minHeight: '100vh', background: '#0f0f0f' }}>
      <Sider
        theme="dark"
        style={{ background: 'rgba(22,22,24,0.95)', borderRight: '1px solid rgba(255,255,255,0.06)' }}
        width={200}
      >
        <div style={{
          fontSize: 16, fontWeight: 800, color: '#fe2c55',
          textAlign: 'center', padding: '20px 16px',
          borderBottom: '1px solid rgba(255,255,255,0.06)', marginBottom: 8,
        }}>
          直播竞拍后台
        </div>
        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ background: 'transparent', borderRight: 'none' }}
        />
      </Sider>
      <Layout>
        <Header style={{
          background: 'rgba(22,22,24,0.95)', backdropFilter: 'blur(20px)',
          padding: '0 24px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center',
          borderBottom: '1px solid rgba(255,255,255,0.06)', height: 56,
        }}>
          <Space size={12}>
            <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13 }}>{nickname}</span>
            <Button ghost size="small" icon={<LogoutOutlined />} style={{ borderRadius: 20, borderColor: 'rgba(255,255,255,0.2)', color: 'rgba(255,255,255,0.6)' }}
              onClick={() => { logout(); navigate('/login') }}>退出</Button>
          </Space>
        </Header>
        <Content style={{ margin: 16 }}>
          <div style={{ padding: 24, background: '#1c1c1e', borderRadius: 12, minHeight: 280, border: '1px solid rgba(255,255,255,0.04)' }}>
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
