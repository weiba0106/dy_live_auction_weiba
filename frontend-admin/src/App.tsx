import { ConfigProvider, theme, Button, Result } from 'antd'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import zhCN from 'antd/locale/zh_CN'
import { useAuthStore } from './stores/auth'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/Login'
import RoomPage from './pages/Room'
import AuctionListPage from './pages/Auction/List'
import AuctionCreatePage from './pages/Auction/Create'
import AuctionDetailPage from './pages/Auction/Detail'
import AuctionEditPage from './pages/Auction/Edit'
import OrderListPage from './pages/Order'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, role } = useAuthStore()
  if (!isLoggedIn()) return <Navigate to="/login" replace />
  if (role !== 1) {
    return (
      <Result
        status="403" title="无权限访问"
        subTitle="此页面为主播管理后台，普通用户请使用移动端参与竞拍"
        extra={<Button type="primary" onClick={() => { useAuthStore.getState().logout(); window.location.href = '/login' }}>返回登录</Button>}
      />
    )
  }
  return <>{children}</>
}

export default function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#fe2c55',
          colorBgContainer: '#1c1c1e',
          colorBgLayout: '#0f0f0f',
          borderRadius: 8,
        },
      }}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<AuthGuard><AppLayout /></AuthGuard>}>
            <Route index element={<Navigate to="/admin/auctions" replace />} />
            <Route path="admin/room" element={<RoomPage />} />
            <Route path="admin/auctions" element={<AuctionListPage />} />
            <Route path="admin/auctions/create" element={<AuctionCreatePage />} />
            <Route path="admin/auctions/:id" element={<AuctionDetailPage />} />
            <Route path="admin/auctions/:id/edit" element={<AuctionEditPage />} />
            <Route path="admin/orders" element={<OrderListPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}
